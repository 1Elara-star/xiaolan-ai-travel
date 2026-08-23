package com.lanyu.xiaolanaitravel.ai.agent.planner;

import com.lanyu.xiaolanaitravel.travel.dto.TravelPlanResponse;
import com.lanyu.xiaolanaitravel.user.dto.UserProfileResponse;

import java.util.List;

/**
 * Planner 使用的受控旅行上下文。
 *
 * <p>这里只包含规划所需字段，不包含用户ID、密码、JWT或外部API密钥。</p>
 */
public record PlannerTravelContext(
        TravelPlanResponse travelPlan,
        UserProfileResponse userProfile,
        List<PlannerMemoryContext> confirmedMemories,
        List<PlannerFavoriteContext> favoriteAttractions
) {
}
