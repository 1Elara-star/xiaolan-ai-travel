package com.lanyu.xiaolanaitravel.ai.agent.planner;

/**
 * Planner Agent 单步决策输入。
 *
 * @param userRequest        用户当前需求
 * @param knownContext       已知的旅行计划、用户画像或地点事实
 * @param previousToolResult 可选的上一次 Tool 结果
 */
public record PlannerAgentRequest(
        String userRequest,
        String knownContext,
        String previousToolResult
) {
}
