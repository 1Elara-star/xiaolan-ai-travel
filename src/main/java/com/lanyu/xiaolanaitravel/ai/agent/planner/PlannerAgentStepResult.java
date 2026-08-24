package com.lanyu.xiaolanaitravel.ai.agent.planner;

/** Planner Agent 一次“决策 + Tool 执行”的结果。 */
public record PlannerAgentStepResult(
        PlannerToolName tool,
        String reason,
        Object toolInput,
        Object toolResult
) {
}
