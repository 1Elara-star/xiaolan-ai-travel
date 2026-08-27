package com.lanyu.xiaolanaitravel.travel;

import com.lanyu.xiaolanaitravel.ai.agent.planner.PlannerWorkflowFacts;
import com.lanyu.xiaolanaitravel.ai.agent.planner.PlannerWorkflowResponse;
import com.lanyu.xiaolanaitravel.ai.agent.planner.PlannerWorkflowService;
import com.lanyu.xiaolanaitravel.ai.agent.planner.PlannerWorkflowStatus;
import com.lanyu.xiaolanaitravel.ai.dto.AiTravelPlanResponse;
import com.lanyu.xiaolanaitravel.explore.service.ExploreService;
import com.lanyu.xiaolanaitravel.favorite.service.AttractionFavoriteService;
import com.lanyu.xiaolanaitravel.travel.dto.TravelDraftSession;
import com.lanyu.xiaolanaitravel.travel.dto.TravelDraftSessionResponse;
import com.lanyu.xiaolanaitravel.travel.dto.TravelPlanDraft;
import com.lanyu.xiaolanaitravel.travel.dto.TravelValidationIssue;
import com.lanyu.xiaolanaitravel.travel.entity.TravelPlan;
import com.lanyu.xiaolanaitravel.travel.service.TravelAiGenerationService;
import com.lanyu.xiaolanaitravel.travel.service.TravelDraftAttractionService;
import com.lanyu.xiaolanaitravel.travel.service.TravelDraftSessionService;
import com.lanyu.xiaolanaitravel.travel.service.TravelPlanDraftService;
import com.lanyu.xiaolanaitravel.travel.service.TravelPlanDraftValidationService;
import com.lanyu.xiaolanaitravel.travel.service.TravelPlanService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TravelAiGenerationServiceTests {

    @Test
    void createsDraftSessionOnlyAfterPlannerReturnsFinalDraft() {
        TravelPlanService planService = mock(TravelPlanService.class);
        PlannerWorkflowService workflowService = mock(PlannerWorkflowService.class);
        TravelPlanDraftService draftService = mock(TravelPlanDraftService.class);
        AttractionFavoriteService favoriteService = mock(AttractionFavoriteService.class);
        TravelDraftAttractionService attractionService = mock(TravelDraftAttractionService.class);
        ExploreService exploreService = mock(ExploreService.class);
        TravelDraftSessionService sessionService = mock(TravelDraftSessionService.class);
        TravelPlanDraftValidationService validationService =
                mock(TravelPlanDraftValidationService.class);

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

        when(workflowService.run(eq(7L), eq(12L), anyString()))
                .thenReturn(completedWorkflow(aiPlan));
        when(planService.getMyPlanById(7L, 12L)).thenReturn(plan);
        when(favoriteService.list(7L)).thenReturn(List.of());
        when(exploreService.listAttractions("南京", null, null)).thenReturn(List.of());
        when(draftService.createDraft(plan, aiPlan)).thenReturn(draft);
        when(attractionService.enrichFromCatalog(
                draft,
                List.of(),
                java.util.Set.of()
        )).thenReturn(draft);
        List<TravelValidationIssue> issues = List.of(
                new TravelValidationIssue(
                        "TIME_CONFLICT",
                        "ERROR",
                        "D1-I2",
                        "D1-I1",
                        "两个行程节点的时间发生重叠"
                )
        );
        when(sessionService.createSession(7L, 12L, draft)).thenReturn(session);
        when(validationService.validate(draft)).thenReturn(issues);
        when(validationService.hasErrors(issues)).thenReturn(true);

        TravelDraftSessionResponse response = new TravelAiGenerationService(
                planService,
                workflowService,
                draftService,
                favoriteService,
                attractionService,
                exploreService,
                sessionService,
                validationService
        ).generateDraftSession(7L, 12L);

        assertEquals("draft-1", response.draftId());
        assertEquals(expiresAt, response.expiresAt());
        assertSame(draft, response.draft());
        assertSame(issues, response.validationIssues());
        assertTrue(response.hasErrors());

        InOrder order = inOrder(
                workflowService,
                planService,
                favoriteService,
                exploreService,
                draftService,
                attractionService,
                validationService,
                sessionService
        );
        order.verify(workflowService).run(eq(7L), eq(12L), anyString());
        order.verify(planService).getMyPlanById(7L, 12L);
        order.verify(favoriteService).list(7L);
        order.verify(exploreService).listAttractions("南京", null, null);
        order.verify(draftService).createDraft(plan, aiPlan);
        order.verify(attractionService).enrichFromCatalog(
                draft,
                List.of(),
                java.util.Set.of()
        );
        order.verify(validationService).validate(draft);
        order.verify(sessionService).createSession(7L, 12L, draft);
    }

    @Test
    void passesAdditionalRequirementsIntoPlannerWorkflow() {
        TravelPlanService planService = mock(TravelPlanService.class);
        PlannerWorkflowService workflowService = mock(PlannerWorkflowService.class);
        TravelPlanDraftService draftService = mock(TravelPlanDraftService.class);
        AttractionFavoriteService favoriteService = mock(AttractionFavoriteService.class);
        TravelDraftAttractionService attractionService = mock(TravelDraftAttractionService.class);
        ExploreService exploreService = mock(ExploreService.class);
        TravelDraftSessionService sessionService = mock(TravelDraftSessionService.class);
        TravelPlanDraftValidationService validationService =
                mock(TravelPlanDraftValidationService.class);

        TravelPlan plan = new TravelPlan();
        plan.setId(12L);
        plan.setDestination("厦门");
        AiTravelPlanResponse aiPlan = new AiTravelPlanResponse();
        TravelPlanDraft draft = new TravelPlanDraft();
        TravelDraftSession session = new TravelDraftSession(
                "draft-2", 7L, 12L, draft,
                LocalDateTime.now(), LocalDateTime.now().plusMinutes(30)
        );
        ArgumentCaptor<String> requestCaptor = ArgumentCaptor.forClass(String.class);

        when(workflowService.run(
                org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq(12L),
                requestCaptor.capture()
        )).thenReturn(completedWorkflow(aiPlan));
        when(planService.getMyPlanById(7L, 12L)).thenReturn(plan);
        when(favoriteService.list(7L)).thenReturn(List.of());
        when(exploreService.listAttractions("厦门", null, null)).thenReturn(List.of());
        when(draftService.createDraft(plan, aiPlan)).thenReturn(draft);
        when(attractionService.enrichFromCatalog(
                draft, List.of(), java.util.Set.of()
        )).thenReturn(draft);
        when(sessionService.createSession(7L, 12L, draft)).thenReturn(session);
        when(validationService.validate(draft)).thenReturn(List.of());

        new TravelAiGenerationService(
                planService,
                workflowService,
                draftService,
                favoriteService,
                attractionService,
                exploreService,
                sessionService,
                validationService
        ).generateDraftSession(
                7L,
                12L,
                "第二天轻松一点，多安排收藏景点"
        );

        assertTrue(requestCaptor.getValue().contains(
                "第二天轻松一点，多安排收藏景点"
        ));
        verify(workflowService).run(7L, 12L, requestCaptor.getValue());
    }

    @Test
    void doesNotCreateDraftWhenPlannerHasNoFinalPlan() {
        TravelPlanService planService = mock(TravelPlanService.class);
        PlannerWorkflowService workflowService = mock(PlannerWorkflowService.class);
        TravelPlanDraftService draftService = mock(TravelPlanDraftService.class);
        AttractionFavoriteService favoriteService = mock(AttractionFavoriteService.class);
        TravelDraftAttractionService attractionService = mock(TravelDraftAttractionService.class);
        ExploreService exploreService = mock(ExploreService.class);
        TravelDraftSessionService sessionService = mock(TravelDraftSessionService.class);
        TravelPlanDraftValidationService validationService =
                mock(TravelPlanDraftValidationService.class);
        PlannerWorkflowResponse failedWorkflow = new PlannerWorkflowResponse(
                PlannerWorkflowStatus.PARTIAL_FAILURE,
                1,
                List.of(),
                new PlannerWorkflowFacts(List.of(), List.of(), List.of()),
                null,
                "外部事实获取失败"
        );
        when(workflowService.run(eq(7L), eq(12L), anyString()))
                .thenReturn(failedWorkflow);

        TravelAiGenerationService service = new TravelAiGenerationService(
                planService,
                workflowService,
                draftService,
                favoriteService,
                attractionService,
                exploreService,
                sessionService,
                validationService
        );

        assertThrows(ResponseStatusException.class,
                () -> service.generateDraftSession(7L, 12L));
        verifyNoInteractions(
                planService,
                draftService,
                favoriteService,
                attractionService,
                exploreService,
                sessionService,
                validationService
        );
    }

    private PlannerWorkflowResponse completedWorkflow(AiTravelPlanResponse finalPlan) {
        return new PlannerWorkflowResponse(
                PlannerWorkflowStatus.COMPLETED,
                0,
                List.of(),
                new PlannerWorkflowFacts(List.of(), List.of(), List.of()),
                finalPlan,
                null
        );
    }
}
