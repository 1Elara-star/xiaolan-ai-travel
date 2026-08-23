package com.lanyu.xiaolanaitravel.ai.agent.planner;

/** 进入 Planner 上下文的已确认长期记忆。 */
public record PlannerMemoryContext(
        String memoryType,
        String memoryContent
) {
}
