package com.lanyu.xiaolanaitravel.ai.agent.planner;

import java.util.List;

/** 进入 Planner 上下文的精简收藏景点信息。 */
public record PlannerFavoriteContext(
        Long attractionId,
        String name,
        String city,
        List<String> tags
) {
}
