package com.lanyu.xiaolanaitravel.ai.agent.planner;

/** Planner Workflow 中一次可追踪的决策与执行记录。 */
public record PlannerWorkflowStep(
        int stepNumber,
        PlannerToolName tool,
        String reason,
        Object toolInput,
        Object toolResult,
        String errorMessage
) {
}
