package com.lanyu.xiaolanaitravel.ai.agent.planner;

import com.lanyu.xiaolanaitravel.ai.service.DeepSeekService;
import com.lanyu.xiaolanaitravel.ai.tool.amap.AmapPoiSearchTool;
import com.lanyu.xiaolanaitravel.ai.tool.amap.AmapTransitRouteTool;
import com.lanyu.xiaolanaitravel.ai.tool.flyai.FlyAiHotelSearchTool;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * 第一版 Planner Agent。
 *
 * <p>每次只让 DeepSeek 选择一个白名单 Tool，然后由后端确定性分发执行。
 * 当前不循环调用、不接 RAG、不修改行程和数据库。</p>
 */
@Service
public class PlannerAgentService {

    private static final int MAX_USER_REQUEST_LENGTH = 2_000;
    private static final int MAX_CONTEXT_LENGTH = 8_000;

    private static final String SYSTEM_PROMPT = """
            你是“小兰 AI Travel”的 Planner Agent，只负责决定下一步是否需要调用一个外部 Tool。

            你每次只能选择以下一个值：
            NONE
            AMAP_POI_SEARCH
            AMAP_TRANSIT_ROUTE
            FLYAI_HOTEL_SEARCH

            决策规则：
            1. 查询具体地点、地址、坐标或图片时，选择 AMAP_POI_SEARCH。
            2. 已经明确拥有起终点坐标和城市编码，需要公交/地铁路线、耗时或首末班时间时，
               选择 AMAP_TRANSIT_ROUTE。
            3. 缺少路线所需坐标时，不得猜测坐标，应先选择 AMAP_POI_SEARCH。
            4. 查询真实酒店候选时，选择 FLYAI_HOTEL_SEARCH。
            5. 不需要外部事实数据时，选择 NONE。
            6. 不允许选择 RAG、天气、机票、火车、门票或其他未开放 Tool。
            7. 不得编造 Tool 结果，也不要生成最终旅行方案。

            必须只返回合法 JSON，结构如下：
            {
              "tool": "NONE",
              "reason": "简短说明为什么这样选择",
              "poiSearch": null,
              "transitRoute": null,
              "hotelSearch": null
            }

            选择某个 Tool 时，只填写对应的参数对象，其他两个参数必须为 null。
            """;

    private final DeepSeekService deepSeekService;
    private final AmapPoiSearchTool amapPoiSearchTool;
    private final AmapTransitRouteTool amapTransitRouteTool;
    private final FlyAiHotelSearchTool flyAiHotelSearchTool;

    public PlannerAgentService(
            DeepSeekService deepSeekService,
            AmapPoiSearchTool amapPoiSearchTool,
            AmapTransitRouteTool amapTransitRouteTool,
            FlyAiHotelSearchTool flyAiHotelSearchTool) {
        this.deepSeekService = deepSeekService;
        this.amapPoiSearchTool = amapPoiSearchTool;
        this.amapTransitRouteTool = amapTransitRouteTool;
        this.flyAiHotelSearchTool = flyAiHotelSearchTool;
    }

    /** 执行一次 Planner 决策；最多调用一个 Tool。 */
    public PlannerAgentStepResult executeNextStep(PlannerAgentRequest request) {
        validateRequest(request);

        PlannerToolDecision decision = deepSeekService.generateStructuredResponse(
                SYSTEM_PROMPT,
                buildUserMessage(request),
                PlannerToolDecision.class
        );
        validateDecision(decision);

        Object toolResult = switch (decision.tool()) {
            case NONE -> null;
            case AMAP_POI_SEARCH -> amapPoiSearchTool.execute(decision.poiSearch());
            case AMAP_TRANSIT_ROUTE -> amapTransitRouteTool.execute(decision.transitRoute());
            case FLYAI_HOTEL_SEARCH -> flyAiHotelSearchTool.execute(decision.hotelSearch());
        };

        return new PlannerAgentStepResult(
                decision.tool(),
                decision.reason().strip(),
                toolResult
        );
    }

    private String buildUserMessage(PlannerAgentRequest request) {
        return """
                【用户当前需求】
                %s

                【当前已知上下文】
                %s

                【上一次 Tool 结果】
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
        if (decision == null || decision.tool() == null) {
            throw invalidDecision("Planner 没有返回有效的 Tool 名称");
        }
        if (decision.reason() == null || decision.reason().isBlank()) {
            throw invalidDecision("Planner 没有说明 Tool 选择原因");
        }

        int parameterCount = countNonNull(
                decision.poiSearch(), decision.transitRoute(), decision.hotelSearch());
        boolean validParameters = switch (decision.tool()) {
            case NONE -> parameterCount == 0;
            case AMAP_POI_SEARCH -> parameterCount == 1 && decision.poiSearch() != null;
            case AMAP_TRANSIT_ROUTE -> parameterCount == 1 && decision.transitRoute() != null;
            case FLYAI_HOTEL_SEARCH -> parameterCount == 1 && decision.hotelSearch() != null;
        };
        if (!validParameters) {
            throw invalidDecision("Planner 返回的 Tool 与参数不一致");
        }
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
