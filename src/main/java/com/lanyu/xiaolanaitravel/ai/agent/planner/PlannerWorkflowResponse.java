package com.lanyu.xiaolanaitravel.ai.agent.planner;

import com.lanyu.xiaolanaitravel.ai.dto.AiTravelPlanResponse;

import java.util.List;

/** 第一版受控 Planner Workflow 的完整返回值。 */
public record PlannerWorkflowResponse(
        PlannerWorkflowStatus status,
        int toolCallCount,
        List<PlannerWorkflowStep> steps,
        PlannerWorkflowFacts facts,
        AiTravelPlanResponse finalPlan,
        String errorMessage
) {
    public PlannerWorkflowResponse {
        steps = steps == null ? List.of() : List.copyOf(steps);
    }
}
