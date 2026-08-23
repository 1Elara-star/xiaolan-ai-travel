package com.lanyu.xiaolanaitravel.ai.agent.planner;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TravelPlannerAgentServiceTests {

    @Mock
    private PlannerContextService plannerContextService;
    @Mock
    private PlannerAgentService plannerAgentService;

    private TravelPlannerAgentService service;

    @BeforeEach
    void setUp() {
        service = new TravelPlannerAgentService(
                plannerContextService,
                plannerAgentService,
                new ObjectMapper().findAndRegisterModules()
        );
    }

    @Test
    void shouldPassServerBuiltContextToPlanner() {
        PlannerTravelContext context = new PlannerTravelContext(
                null,
                null,
                List.of(new PlannerMemoryContext("PACE", "不要太赶")),
                List.of(new PlannerFavoriteContext(
                        8L, "鼓浪屿", "厦门", List.of("海岛")))
        );
        PlannerAgentStepResult expected = new PlannerAgentStepResult(
                PlannerToolName.AMAP_POI_SEARCH, "需要真实坐标", null);
        when(plannerContextService.build(7L, 12L)).thenReturn(context);
        when(plannerAgentService.executeNextStep(
                org.mockito.ArgumentMatchers.any(PlannerAgentRequest.class)))
                .thenReturn(expected);

        PlannerAgentStepResult result = service.executeStep(
                7L, 12L, "帮我确认鼓浪屿的位置");

        ArgumentCaptor<PlannerAgentRequest> captor =
                ArgumentCaptor.forClass(PlannerAgentRequest.class);
        verify(plannerAgentService).executeNextStep(captor.capture());
        assertSame(expected, result);
        assertEquals("帮我确认鼓浪屿的位置", captor.getValue().userRequest());
        assertFalse(captor.getValue().knownContext().contains("password"));
        assertFalse(captor.getValue().knownContext().contains("userId"));
        assertEquals("鼓浪屿", context.favoriteAttractions().get(0).name());
    }

    @Test
    void shouldStopBeforePlannerWhenPlanIsNotOwned() {
        ResponseStatusException notFound =
                new ResponseStatusException(HttpStatus.NOT_FOUND, "旅行计划不存在");
        when(plannerContextService.build(9L, 12L)).thenThrow(notFound);

        ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
                () -> service.executeStep(9L, 12L, "查一下路线"));

        assertSame(notFound, thrown);
        verify(plannerAgentService, never()).executeNextStep(
                org.mockito.ArgumentMatchers.any(PlannerAgentRequest.class));
    }
}
