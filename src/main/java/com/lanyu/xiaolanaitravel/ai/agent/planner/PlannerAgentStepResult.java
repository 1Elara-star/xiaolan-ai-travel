package com.lanyu.xiaolanaitravel.ai.agent.planner;

import com.lanyu.xiaolanaitravel.ai.dto.AiTravelPlanResponse;

/** Planner Agent 一次“决策 + Tool 执行”的结果。 */
public record PlannerAgentStepResult(
        PlannerActionType action,
        PlannerToolName tool,
        String reason,
        Object toolInput,
        Object toolResult,
        AiTravelPlanResponse finalPlan
) {
}
