package com.lanyu.xiaolanaitravel.ai.agent.planner;

import com.lanyu.xiaolanaitravel.explore.dto.AttractionResponse;
import com.lanyu.xiaolanaitravel.favorite.dto.FavoriteAttractionResponse;
import com.lanyu.xiaolanaitravel.favorite.service.AttractionFavoriteService;
import com.lanyu.xiaolanaitravel.memory.dto.UserMemoryResponse;
import com.lanyu.xiaolanaitravel.memory.service.UserMemoryService;
import com.lanyu.xiaolanaitravel.travel.dto.TravelPlanResponse;
import com.lanyu.xiaolanaitravel.travel.entity.TravelPlan;
import com.lanyu.xiaolanaitravel.travel.service.TravelPlanService;
import com.lanyu.xiaolanaitravel.user.dto.UserProfileResponse;
import com.lanyu.xiaolanaitravel.user.entity.UserProfile;
import com.lanyu.xiaolanaitravel.user.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlannerContextServiceTests {

    @Mock
    private TravelPlanService travelPlanService;
    @Mock
    private UserProfileService userProfileService;
    @Mock
    private UserMemoryService userMemoryService;
    @Mock
    private AttractionFavoriteService attractionFavoriteService;

    private PlannerContextService service;

    @BeforeEach
    void setUp() {
        service = new PlannerContextService(
                travelPlanService,
                userProfileService,
                userMemoryService,
                attractionFavoriteService
        );
    }

    @Test
    void shouldBuildOwnedPlanProfileConfirmedMemoryAndFavoriteContext() {
        TravelPlan plan = new TravelPlan();
        plan.setId(12L);
        UserProfile profile = new UserProfile();
        profile.setUserId(7L);
        TravelPlanResponse planResponse = planResponse();
        UserProfileResponse profileResponse = profileResponse();

        when(travelPlanService.getMyPlanById(7L, 12L)).thenReturn(plan);
        when(travelPlanService.toResponse(plan)).thenReturn(planResponse);
        when(userProfileService.getProfile(7L)).thenReturn(profile);
        when(userProfileService.toResponse(profile)).thenReturn(profileResponse);
        when(userMemoryService.list(7L, null, true)).thenReturn(List.of(
                new UserMemoryResponse(2L, "PACE", "不要排得太满", true,
                        LocalDateTime.now(), LocalDateTime.now())));
        when(attractionFavoriteService.list(7L)).thenReturn(List.of(
                new FavoriteAttractionResponse(3L, LocalDateTime.now(), attraction())));

        PlannerTravelContext context = service.build(7L, 12L);

        assertSame(planResponse, context.travelPlan());
        assertSame(profileResponse, context.userProfile());
        assertEquals("不要排得太满", context.confirmedMemories().get(0).memoryContent());
        assertEquals("鼓浪屿", context.favoriteAttractions().get(0).name());
        assertEquals(List.of("海岛", "建筑"), context.favoriteAttractions().get(0).tags());
        verify(userMemoryService).list(7L, null, true);
    }

    private TravelPlanResponse planResponse() {
        return new TravelPlanResponse(
                12L, "厦门旅行", "南京", "厦门",
                LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 23),
                5, 2, "朋友", null, "休闲旅行", "喜欢海边",
                "不要太赶", "PLANNING", null, null);
    }

    private UserProfileResponse profileResponse() {
        return new UserProfileResponse(
                "INFP", "轻松", "性价比", "少换乘", "海边,人文", "排队",
                null, null, "本地菜", null, null, "交通方便");
    }

    private AttractionResponse attraction() {
        return new AttractionResponse(
                8L, "鼓浪屿", "厦门", null, "历史人文", null,
                null, null, List.of("海岛", "建筑"), null,
                null, null, null, null, null, "ATTRACTION", null, null);
    }
}
