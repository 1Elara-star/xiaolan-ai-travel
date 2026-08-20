package com.lanyu.xiaolanaitravel.travel;

import com.lanyu.xiaolanaitravel.ai.dto.AiTravelPlanResponse;
import com.lanyu.xiaolanaitravel.ai.service.DeepSeekService;
import com.lanyu.xiaolanaitravel.amap.service.AmapService;
import com.lanyu.xiaolanaitravel.favorite.service.AttractionFavoriteService;
import com.lanyu.xiaolanaitravel.explore.service.ExploreService;
import com.lanyu.xiaolanaitravel.travel.dto.TravelDraftSession;
import com.lanyu.xiaolanaitravel.travel.dto.TravelDraftSessionResponse;
import com.lanyu.xiaolanaitravel.travel.dto.TravelPlanDraft;
import com.lanyu.xiaolanaitravel.travel.entity.TravelPlan;
import com.lanyu.xiaolanaitravel.travel.service.TravelAiGenerationService;
import com.lanyu.xiaolanaitravel.travel.service.TravelDraftAttractionService;
import com.lanyu.xiaolanaitravel.travel.service.TravelDraftSessionService;
import com.lanyu.xiaolanaitravel.travel.service.TravelPlanDraftService;
import com.lanyu.xiaolanaitravel.travel.service.TravelPlanService;
import com.lanyu.xiaolanaitravel.user.service.UserProfileService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TravelAiGenerationServiceTests {

    @Test
    void generatesAndCreatesDraftSessionWithoutCallingAmap() {
        TravelPlanService planService = mock(TravelPlanService.class);
        DeepSeekService deepSeekService = mock(DeepSeekService.class);
        TravelPlanDraftService draftService = mock(TravelPlanDraftService.class);
        AttractionFavoriteService favoriteService = mock(AttractionFavoriteService.class);
        UserProfileService profileService = mock(UserProfileService.class);
        TravelDraftAttractionService attractionService = mock(TravelDraftAttractionService.class);
        ExploreService exploreService = mock(ExploreService.class);
        TravelDraftSessionService sessionService = mock(TravelDraftSessionService.class);
        AmapService amapService = mock(AmapService.class);
        when(amapService.searchPois(
                anyString(),
                anyString(),
                anyInt()
        )).thenThrow(new IllegalStateException("模拟高德不可用"));

        TravelPlan plan = new TravelPlan();
        plan.setId(12L);
        plan.setDestination("南京");
        plan.setTravelDays(2);
        AiTravelPlanResponse aiPlan = new AiTravelPlanResponse();
        TravelPlanDraft draft = new TravelPlanDraft();
        LocalDateTime expiresAt = LocalDateTime.of(2026, 8, 19, 12, 30);
        TravelDraftSession session = new TravelDraftSession(
                "draft-1",
                7L,
                12L,
                draft,
                expiresAt.minusMinutes(30),
                expiresAt
        );

        when(planService.getMyPlanById(7L, 12L)).thenReturn(plan);
        when(profileService.getProfile(7L)).thenReturn(null);
        when(favoriteService.list(7L)).thenReturn(List.of());
        when(exploreService.listAttractions("南京", null, null))
                .thenReturn(List.of());
        when(deepSeekService.generateTravelPlan(anyString())).thenReturn(aiPlan);
        when(draftService.createDraft(plan, aiPlan)).thenReturn(draft);
        when(attractionService.enrichFromCatalog(
                draft,
                List.of(),
                java.util.Set.of()
        )).thenReturn(draft);
        when(sessionService.createSession(7L, 12L, draft)).thenReturn(session);

        TravelDraftSessionResponse response =
                new TravelAiGenerationService(
                        planService,
                        deepSeekService,
                        draftService,
                        favoriteService,
                        profileService,
                        attractionService,
                        exploreService,
                        sessionService
                ).generateDraftSession(7L, 12L);

        assertEquals("draft-1", response.draftId());
        assertEquals(expiresAt, response.expiresAt());
        assertSame(draft, response.draft());

        InOrder order = inOrder(
                planService,
                profileService,
                favoriteService,
                exploreService,
                deepSeekService,
                draftService,
                attractionService,
                sessionService
        );
        order.verify(planService).getMyPlanById(7L, 12L);
        order.verify(profileService).getProfile(7L);
        order.verify(favoriteService).list(7L);
        order.verify(exploreService).listAttractions("南京", null, null);
        order.verify(deepSeekService).generateTravelPlan(anyString());
        order.verify(draftService).createDraft(plan, aiPlan);
        order.verify(attractionService).enrichFromCatalog(
                draft,
                List.of(),
                java.util.Set.of()
        );
        order.verify(sessionService).createSession(7L, 12L, draft);
        verifyNoInteractions(amapService);
    }

    @Test
    void includesUserAdditionalRequirementsInPrompt() {
        TravelPlanService planService = mock(TravelPlanService.class);
        DeepSeekService deepSeekService = mock(DeepSeekService.class);
        TravelPlanDraftService draftService = mock(TravelPlanDraftService.class);
        AttractionFavoriteService favoriteService = mock(AttractionFavoriteService.class);
        UserProfileService profileService = mock(UserProfileService.class);
        TravelDraftAttractionService attractionService = mock(TravelDraftAttractionService.class);
        ExploreService exploreService = mock(ExploreService.class);
        TravelDraftSessionService sessionService = mock(TravelDraftSessionService.class);

        TravelPlan plan = new TravelPlan();
        plan.setId(12L);
        plan.setDestination("厦门");
        plan.setTravelDays(1);
        TravelPlanDraft draft = new TravelPlanDraft();
        AiTravelPlanResponse aiPlan = new AiTravelPlanResponse();
        TravelDraftSession session = new TravelDraftSession(
                "draft-2", 7L, 12L, draft,
                LocalDateTime.now(), LocalDateTime.now().plusMinutes(30)
        );

        when(planService.getMyPlanById(7L, 12L)).thenReturn(plan);
        when(profileService.getProfile(7L)).thenReturn(null);
        when(favoriteService.list(7L)).thenReturn(List.of());
        when(exploreService.listAttractions("厦门", null, null)).thenReturn(List.of());
        org.mockito.ArgumentCaptor<String> promptCaptor =
                org.mockito.ArgumentCaptor.forClass(String.class);
        when(deepSeekService.generateTravelPlan(promptCaptor.capture())).thenReturn(aiPlan);
        when(draftService.createDraft(plan, aiPlan)).thenReturn(draft);
        when(attractionService.enrichFromCatalog(
                draft, List.of(), java.util.Set.of()
        )).thenReturn(draft);
        when(sessionService.createSession(7L, 12L, draft)).thenReturn(session);

        new TravelAiGenerationService(
                planService, deepSeekService, draftService,
                favoriteService, profileService, attractionService,
                exploreService, sessionService
        ).generateDraftSession(
                7L,
                12L,
                "第二天轻松一点，多安排收藏景点"
        );

        assertTrue(promptCaptor.getValue().contains(
                "第二天轻松一点，多安排收藏景点"
        ));
    }
}
