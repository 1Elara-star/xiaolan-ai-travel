package com.lanyu.xiaolanaitravel.ai.agent.planner;

import com.lanyu.xiaolanaitravel.ai.service.DeepSeekService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * 第一版 Planner Agent。
 *
 * <p>每轮只允许 DeepSeek 选择一个白名单 Tool，或在事实充分时返回最终结构化行程。
 * Tool 由后端确定性分发执行；当前不接 RAG、不执行 Repair，也不写数据库。</p>
 */
@Service
public class PlannerAgentService {

    private static final int MAX_USER_REQUEST_LENGTH = 2_000;
    private static final int MAX_CONTEXT_LENGTH = 8_000;

    private static final String SYSTEM_PROMPT = """
            你是“小兰 AI Travel”的 Planner Agent。你需要综合用户旅行计划、用户画像、
            已确认的长期记忆、收藏景点、本次补充要求，以及前几轮已经取得的 Tool 事实，
            决定下一步继续查询真实数据，或者输出最终结构化候选行程。

            每一轮 action 只能选择以下一个值：
            CALL_TOOL
            FINAL_DRAFT

            当 action=CALL_TOOL 时，tool 只能选择以下一个值：
            AMAP_POI_SEARCH
            AMAP_TRANSIT_ROUTE
            FLYAI_HOTEL_SEARCH

            Tool 决策规则：
            1. 查询具体地点、地址、坐标或图片时，选择 AMAP_POI_SEARCH。
            2. 已经明确拥有起终点坐标和城市编码，需要公交/地铁路线、耗时或首末班时间时，
               选择 AMAP_TRANSIT_ROUTE。
            3. 缺少路线所需坐标时，不得猜测坐标，应先选择 AMAP_POI_SEARCH。
            4. 查询真实酒店候选时，选择 FLYAI_HOTEL_SEARCH。
            5. 不允许选择 RAG、天气、机票、火车、门票或其他未开放 Tool。
            6. 不得重复调用已经使用完全相同参数执行成功的 Tool。
            7. 不得编造 Tool 结果。

            需要 Tool 时，只返回以下结构的合法 JSON：
            {
              "action": "CALL_TOOL",
              "tool": "AMAP_POI_SEARCH",
              "reason": "简短说明为什么这样选择",
              "poiSearch": {
                "keyword": "鼓浪屿",
                "city": "厦门",
                "limit": 3
              },
              "transitRoute": null,
              "hotelSearch": null,
              "finalPlan": null
            }

            选择某个 Tool 时，只填写对应的参数对象，其他两个参数必须为 null。

            当已有信息足以生成方案时，action 必须选择 FINAL_DRAFT，tool 必须为 NONE，
            三个 Tool 参数必须全部为 null，并在 finalPlan 中返回完整结构化行程：
            {
              "action": "FINAL_DRAFT",
              "tool": "NONE",
              "reason": "已有足够信息，可以生成候选行程",
              "poiSearch": null,
              "transitRoute": null,
              "hotelSearch": null,
              "finalPlan": {
                "destination": "目的地",
                "travelDays": 3,
                "summary": "行程整体说明",
                "days": [
                  {
                    "dayNumber": 1,
                    "theme": "当天主题",
                    "items": [
                      {
                        "attractionId": null,
                        "placeName": "具体地点名称",
                        "startTime": "09:00",
                        "endTime": "11:00",
                        "endDayOffset": 0,
                        "itemType": "ATTRACTION",
                        "description": "结合偏好和事实说明安排原因"
                      }
                    ]
                  }
                ]
              }
            }

            最终行程规则：
            1. 必须遵守计划中的目的地、旅行天数、预算、同行情况和用户明确约束。
            2. 必须结合用户画像、已确认记忆和收藏，但不要为了加入全部收藏让行程过满。
            3. dayNumber 从1开始连续；每天节点按照时间顺序排列。
            4. startTime、endTime 使用HH:mm；跨午夜时 endDayOffset=1，否则为0。
            5. itemType 只能是 ATTRACTION、FOOD、HOTEL、EVENT、REST、OTHER。
            6. 使用上下文中带 attractionId 的景点时必须返回真实ID，否则填写null。
            7. 不得编造精确路线、距离、耗时、票价、营业时间或酒店价格。
            8. 没有真实酒店候选时，酒店名称使用“待推荐酒店”，不得虚构具体酒店。
            9. 优先使用已经由 Tool 核验的具体地点和事实；不能把 Tool 事实张冠李戴。
            10. FINAL_DRAFT 必须一次返回完整行程，不要返回Markdown或JSON之外的文字。
            """;

    private final DeepSeekService deepSeekService;
    private final PlannerToolExecutor plannerToolExecutor;

    public PlannerAgentService(
            DeepSeekService deepSeekService,
            PlannerToolExecutor plannerToolExecutor) {
        this.deepSeekService = deepSeekService;
        this.plannerToolExecutor = plannerToolExecutor;
    }

    /** 执行一次 Planner 决策；最多调用一个 Tool。 */
    public PlannerAgentStepResult executeNextStep(PlannerAgentRequest request) {
        return executeDecision(decideNextStep(request));
    }

    /** 只让 Planner 选择下一步，不执行任何 Tool。 */
    public PlannerToolDecision decideNextStep(PlannerAgentRequest request) {
        validateRequest(request);

        PlannerToolDecision decision = deepSeekService.generateStructuredResponse(
                SYSTEM_PROMPT,
                buildUserMessage(request),
                PlannerToolDecision.class
        );
        validateDecision(decision);
        return decision;
    }

    /** 执行一项已经通过白名单与参数校验的 Planner 决策。 */
    public PlannerAgentStepResult executeDecision(PlannerToolDecision decision) {
        validateDecision(decision);
        if (decision.action() == PlannerActionType.FINAL_DRAFT) {
            return new PlannerAgentStepResult(
                    decision.action(),
                    decision.tool(),
                    decision.reason().strip(),
                    null,
                    null,
                    decision.finalPlan()
            );
        }

        Object toolResult = plannerToolExecutor.execute(decision);

        return new PlannerAgentStepResult(
                decision.action(),
                decision.tool(),
                decision.reason().strip(),
                toolInput(decision),
                toolResult,
                null
        );
    }

    private Object toolInput(PlannerToolDecision decision) {
        return switch (decision.tool()) {
            case NONE -> null;
            case AMAP_POI_SEARCH -> decision.poiSearch();
            case AMAP_TRANSIT_ROUTE -> decision.transitRoute();
            case FLYAI_HOTEL_SEARCH -> decision.hotelSearch();
        };
    }

    private String buildUserMessage(PlannerAgentRequest request) {
        return """
                【用户当前需求】
                %s

                【当前已知上下文】
                %s

                【本次 Workflow 已取得的 Tool 结果】
                %s
                """.formatted(
                request.userRequest().strip(),
                normalizeContext(request.knownContext()),
                normalizeContext(request.previousToolResult())
        );
    }

    private void validateRequest(PlannerAgentRequest request) {
        if (request == null || request.userRequest() == null
                || request.userRequest().isBlank()) {
            throw new IllegalArgumentException("Planner 用户需求不能为空");
        }
        if (request.userRequest().length() > MAX_USER_REQUEST_LENGTH) {
            throw new IllegalArgumentException("Planner 用户需求不能超过2000个字符");
        }
        if (length(request.knownContext()) > MAX_CONTEXT_LENGTH
                || length(request.previousToolResult()) > MAX_CONTEXT_LENGTH) {
            throw new IllegalArgumentException("Planner 上下文不能超过8000个字符");
        }
    }

    private void validateDecision(PlannerToolDecision decision) {
        if (decision == null || decision.action() == null || decision.tool() == null) {
            throw invalidDecision("Planner 没有返回有效的动作或 Tool 名称");
        }
        if (decision.reason() == null || decision.reason().isBlank()) {
            throw invalidDecision("Planner 没有说明 Tool 选择原因");
        }

        int parameterCount = countNonNull(
                decision.poiSearch(), decision.transitRoute(), decision.hotelSearch());
        boolean validParameters = switch (decision.action()) {
            case CALL_TOOL -> decision.finalPlan() == null
                    && decision.tool() != PlannerToolName.NONE
                    && matchesSelectedTool(decision, parameterCount);
            case FINAL_DRAFT -> decision.tool() == PlannerToolName.NONE
                    && parameterCount == 0
                    && isCompleteFinalPlan(decision);
        };
        if (!validParameters) {
            throw invalidDecision("Planner 返回的动作、Tool、参数或最终方案不一致");
        }
    }

    private boolean matchesSelectedTool(PlannerToolDecision decision, int parameterCount) {
        if (parameterCount != 1) {
            return false;
        }
        return switch (decision.tool()) {
            case NONE -> false;
            case AMAP_POI_SEARCH -> decision.poiSearch() != null;
            case AMAP_TRANSIT_ROUTE -> decision.transitRoute() != null;
            case FLYAI_HOTEL_SEARCH -> decision.hotelSearch() != null;
        };
    }

    private boolean isCompleteFinalPlan(PlannerToolDecision decision) {
        return decision.finalPlan() != null
                && decision.finalPlan().getDestination() != null
                && !decision.finalPlan().getDestination().isBlank()
                && decision.finalPlan().getTravelDays() != null
                && decision.finalPlan().getDays() != null
                && !decision.finalPlan().getDays().isEmpty();
    }

    private int countNonNull(Object... values) {
        int count = 0;
        for (Object value : values) {
            if (value != null) {
                count++;
            }
        }
        return count;
    }

    private int length(String value) {
        return value == null ? 0 : value.length();
    }

    private String normalizeContext(String value) {
        return value == null || value.isBlank() ? "无" : value.strip();
    }

    private ResponseStatusException invalidDecision(String message) {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, message);
    }
}
