package com.lanyu.xiaolanaitravel.ai.agent.planner;

/** 第一版 Planner Workflow 的结束状态。 */
public enum PlannerWorkflowStatus {
    COMPLETED,
    STEP_LIMIT_REACHED,
    REPEATED_TOOL_CALL_BLOCKED,
    PARTIAL_FAILURE
}
