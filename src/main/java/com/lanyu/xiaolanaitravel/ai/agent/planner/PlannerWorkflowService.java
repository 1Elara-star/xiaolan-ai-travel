package com.lanyu.xiaolanaitravel.ai.agent.planner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lanyu.xiaolanaitravel.ai.dto.AiTravelPlanResponse;
import com.lanyu.xiaolanaitravel.ai.tool.amap.dto.AmapPoiSearchToolResult;
import com.lanyu.xiaolanaitravel.ai.tool.amap.dto.AmapTransitRouteToolResult;
import com.lanyu.xiaolanaitravel.ai.tool.flyai.dto.FlyAiHotelSearchToolResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 第一版受控 Planner Workflow。
 *
 * <p>最多执行五次白名单 Tool；每一步只执行一个 Tool。每次执行结果都会重新
 * 交给同一个 Planner 角色，直到返回最终结构化候选行程。当前不写数据库，
 * 也不接入 RAG、固定校验或 Repair。</p>
 */
@Service
public class PlannerWorkflowService {

    static final int MAX_TOOL_CALLS = 5;
    private static final int MAX_PROMPT_CONTEXT_LENGTH = 7_800;

    private final PlannerContextService plannerContextService;
    private final PlannerAgentService plannerAgentService;
    private final ObjectMapper objectMapper;

    public PlannerWorkflowService(
            PlannerContextService plannerContextService,
            PlannerAgentService plannerAgentService,
            ObjectMapper objectMapper) {
        this.plannerContextService = plannerContextService;
        this.plannerAgentService = plannerAgentService;
        this.objectMapper = objectMapper;
    }

    public PlannerWorkflowResponse run(Long userId, Long planId, String userRequest) {
        PlannerTravelContext context = plannerContextService.build(userId, planId);
        String knownContext = serializeForPrompt(context);
        List<PlannerWorkflowStep> steps = new ArrayList<>();
        List<AmapPoiSearchToolResult> poiFacts = new ArrayList<>();
        List<AmapTransitRouteToolResult> transitFacts = new ArrayList<>();
        List<FlyAiHotelSearchToolResult> hotelFacts = new ArrayList<>();
        Set<String> executedFingerprints = new HashSet<>();
        int toolCallCount = 0;

        while (true) {
            PlannerToolDecision decision;
            try {
                decision = plannerAgentService.decideNextStep(new PlannerAgentRequest(
                        userRequest,
                        knownContext,
                        serializeForPrompt(steps)
                ));
            } catch (RuntimeException exception) {
                return response(PlannerWorkflowStatus.PARTIAL_FAILURE, toolCallCount,
                        steps, poiFacts, transitFacts, hotelFacts,
                        null,
                        "Planner决策失败，请稍后重试");
            }

            if (decision.action() == PlannerActionType.FINAL_DRAFT) {
                steps.add(new PlannerWorkflowStep(
                        steps.size() + 1,
                        decision.action(),
                        PlannerToolName.NONE,
                        decision.reason().strip(),
                        null,
                        null,
                        null
                ));
                return response(PlannerWorkflowStatus.COMPLETED, toolCallCount,
                        steps, poiFacts, transitFacts, hotelFacts,
                        decision.finalPlan(), null);
            }

            if (toolCallCount >= MAX_TOOL_CALLS) {
                return response(PlannerWorkflowStatus.STEP_LIMIT_REACHED, toolCallCount,
                        steps, poiFacts, transitFacts, hotelFacts,
                        null,
                        "已达到单次Workflow最多5次Tool调用限制，尚未生成最终方案");
            }

            Object toolInput = toolInput(decision);
            String fingerprint = fingerprint(decision.tool(), toolInput);
            if (!executedFingerprints.add(fingerprint)) {
                steps.add(new PlannerWorkflowStep(
                        steps.size() + 1,
                        decision.action(),
                        decision.tool(),
                        decision.reason().strip(),
                        toolInput,
                        null,
                        "已阻止完全相同的重复Tool调用"
                ));
                return response(PlannerWorkflowStatus.REPEATED_TOOL_CALL_BLOCKED,
                        toolCallCount, steps, poiFacts, transitFacts, hotelFacts,
                        null,
                        "Planner尝试重复调用相同Tool");
            }

            try {
                PlannerAgentStepResult result = plannerAgentService.executeDecision(decision);
                toolCallCount++;
                steps.add(new PlannerWorkflowStep(
                        steps.size() + 1,
                        result.action(),
                        result.tool(),
                        result.reason(),
                        result.toolInput(),
                        result.toolResult(),
                        null
                ));
                collectFact(result.toolResult(), poiFacts, transitFacts, hotelFacts);
            } catch (RuntimeException exception) {
                steps.add(new PlannerWorkflowStep(
                        steps.size() + 1,
                        decision.action(),
                        decision.tool(),
                        decision.reason().strip(),
                        toolInput,
                        null,
                        "外部Tool执行失败，请稍后重试"
                ));
                return response(PlannerWorkflowStatus.PARTIAL_FAILURE, toolCallCount,
                        steps, poiFacts, transitFacts, hotelFacts,
                        null,
                        "部分外部事实获取失败，已保留先前成功结果");
            }
        }
    }

    private Object toolInput(PlannerToolDecision decision) {
        return switch (decision.tool()) {
            case NONE -> null;
            case AMAP_POI_SEARCH -> decision.poiSearch();
            case AMAP_TRANSIT_ROUTE -> decision.transitRoute();
            case FLYAI_HOTEL_SEARCH -> decision.hotelSearch();
        };
    }

    private void collectFact(
            Object result,
            List<AmapPoiSearchToolResult> poiFacts,
            List<AmapTransitRouteToolResult> transitFacts,
            List<FlyAiHotelSearchToolResult> hotelFacts) {
        if (result instanceof AmapPoiSearchToolResult poiResult) {
            poiFacts.add(poiResult);
        } else if (result instanceof AmapTransitRouteToolResult transitResult) {
            transitFacts.add(transitResult);
        } else if (result instanceof FlyAiHotelSearchToolResult hotelResult) {
            hotelFacts.add(hotelResult);
        }
    }

    private PlannerWorkflowResponse response(
            PlannerWorkflowStatus status,
            int toolCallCount,
            List<PlannerWorkflowStep> steps,
            List<AmapPoiSearchToolResult> poiFacts,
            List<AmapTransitRouteToolResult> transitFacts,
            List<FlyAiHotelSearchToolResult> hotelFacts,
            AiTravelPlanResponse finalPlan,
            String errorMessage) {
        return new PlannerWorkflowResponse(
                status,
                toolCallCount,
                steps,
                new PlannerWorkflowFacts(poiFacts, transitFacts, hotelFacts),
                finalPlan,
                errorMessage
        );
    }

    private String fingerprint(PlannerToolName tool, Object input) {
        return tool.name() + ":" + serialize(input);
    }

    private String serializeForPrompt(Object value) {
        String serialized = serialize(value);
        if (serialized.length() <= MAX_PROMPT_CONTEXT_LENGTH) {
            return serialized;
        }
        return serialized.substring(0, MAX_PROMPT_CONTEXT_LENGTH) + "【内容已截断】";
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Planner Workflow数据序列化失败", exception);
        }
    }
}
