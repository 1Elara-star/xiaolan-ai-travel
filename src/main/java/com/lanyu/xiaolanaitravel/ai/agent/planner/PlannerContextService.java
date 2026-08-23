package com.lanyu.xiaolanaitravel.ai.agent.planner;

import com.lanyu.xiaolanaitravel.favorite.service.AttractionFavoriteService;
import com.lanyu.xiaolanaitravel.memory.service.UserMemoryService;
import com.lanyu.xiaolanaitravel.travel.entity.TravelPlan;
import com.lanyu.xiaolanaitravel.travel.service.TravelPlanService;
import com.lanyu.xiaolanaitravel.user.entity.UserProfile;
import com.lanyu.xiaolanaitravel.user.service.UserProfileService;
import org.springframework.stereotype.Service;

/** 为 Planner 组装当前用户真实、精简且不含敏感字段的旅行上下文。 */
@Service
public class PlannerContextService {

    private static final int MAX_MEMORIES = 20;
    private static final int MAX_FAVORITES = 20;

    private final TravelPlanService travelPlanService;
    private final UserProfileService userProfileService;
    private final UserMemoryService userMemoryService;
    private final AttractionFavoriteService attractionFavoriteService;

    public PlannerContextService(
            TravelPlanService travelPlanService,
            UserProfileService userProfileService,
            UserMemoryService userMemoryService,
            AttractionFavoriteService attractionFavoriteService) {
        this.travelPlanService = travelPlanService;
        this.userProfileService = userProfileService;
        this.userMemoryService = userMemoryService;
        this.attractionFavoriteService = attractionFavoriteService;
    }

    public PlannerTravelContext build(Long userId, Long planId) {
        TravelPlan plan = travelPlanService.getMyPlanById(userId, planId);
        UserProfile profile = userProfileService.getProfile(userId);

        var memories = userMemoryService.list(userId, null, true).stream()
                .limit(MAX_MEMORIES)
                .map(memory -> new PlannerMemoryContext(
                        memory.memoryType(), memory.memoryContent()))
                .toList();

        var favorites = attractionFavoriteService.list(userId).stream()
                .limit(MAX_FAVORITES)
                .map(favorite -> new PlannerFavoriteContext(
                        favorite.attraction().id(),
                        favorite.attraction().name(),
                        favorite.attraction().city(),
                        favorite.attraction().tags()))
                .toList();

        return new PlannerTravelContext(
                travelPlanService.toResponse(plan),
                userProfileService.toResponse(profile),
                memories,
                favorites
        );
    }
}
