package com.lanyu.xiaolanaitravel.ai.agent.planner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lanyu.xiaolanaitravel.ai.dto.AiTravelDay;
import com.lanyu.xiaolanaitravel.ai.dto.AiTravelPlanResponse;
import com.lanyu.xiaolanaitravel.ai.tool.amap.dto.AmapPoiSearchToolRequest;
import com.lanyu.xiaolanaitravel.ai.tool.amap.dto.AmapPoiSearchToolResult;
import com.lanyu.xiaolanaitravel.ai.tool.flyai.dto.FlyAiHotelSearchToolRequest;
import com.lanyu.xiaolanaitravel.ai.tool.flyai.dto.FlyAiHotelSearchToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlannerWorkflowServiceTests {

    @Mock
    private PlannerContextService plannerContextService;
    @Mock
    private PlannerAgentService plannerAgentService;

    private PlannerWorkflowService service;

    @BeforeEach
    void setUp() {
        service = new PlannerWorkflowService(
                plannerContextService,
                plannerAgentService,
                new ObjectMapper().findAndRegisterModules()
        );
        when(plannerContextService.build(7L, 12L)).thenReturn(emptyContext());
    }

    @Test
    void shouldExecuteMultipleDifferentToolsUntilPlannerReturnsFinalDraft() {
        AmapPoiSearchToolRequest poiInput =
                new AmapPoiSearchToolRequest("鼓浪屿", "厦门", 3);
        AmapPoiSearchToolResult poiResult = poiResult("B001", "鼓浪屿");
        FlyAiHotelSearchToolRequest hotelInput =
                new FlyAiHotelSearchToolRequest("厦门", "中山路", 600, 5);
        FlyAiHotelSearchToolResult hotelResult =
                new FlyAiHotelSearchToolResult(List.of(), 0);
        PlannerToolDecision poiDecision = new PlannerToolDecision(
                PlannerActionType.CALL_TOOL,
                PlannerToolName.AMAP_POI_SEARCH, "需要地点坐标",
                poiInput, null, null, null);
        PlannerToolDecision hotelDecision = new PlannerToolDecision(
                PlannerActionType.CALL_TOOL,
                PlannerToolName.FLYAI_HOTEL_SEARCH, "需要酒店候选",
                null, null, hotelInput, null);
        AiTravelPlanResponse finalPlan = validFinalPlan();
        PlannerToolDecision doneDecision = new PlannerToolDecision(
                PlannerActionType.FINAL_DRAFT,
                PlannerToolName.NONE, "事实已经足够",
                null, null, null, finalPlan);
        when(plannerAgentService.decideNextStep(any(PlannerAgentRequest.class)))
                .thenReturn(poiDecision, hotelDecision, doneDecision);
        when(plannerAgentService.executeDecision(poiDecision)).thenReturn(
                new PlannerAgentStepResult(
                        PlannerActionType.CALL_TOOL,
                        PlannerToolName.AMAP_POI_SEARCH, "需要地点坐标",
                        poiInput, poiResult, null));
        when(plannerAgentService.executeDecision(hotelDecision)).thenReturn(
                new PlannerAgentStepResult(
                        PlannerActionType.CALL_TOOL,
                        PlannerToolName.FLYAI_HOTEL_SEARCH, "需要酒店候选",
                        hotelInput, hotelResult, null));

        PlannerWorkflowResponse response = service.run(7L, 12L, "安排厦门旅行");

        assertEquals(PlannerWorkflowStatus.COMPLETED, response.status());
        assertEquals(2, response.toolCallCount());
        assertEquals(3, response.steps().size());
        assertEquals(1, response.facts().poiResults().size());
        assertEquals(1, response.facts().hotelResults().size());
        assertSame(finalPlan, response.finalPlan());
        verify(plannerAgentService, never()).executeDecision(doneDecision);

        ArgumentCaptor<PlannerAgentRequest> requestCaptor =
                ArgumentCaptor.forClass(PlannerAgentRequest.class);
        verify(plannerAgentService, times(3)).decideNextStep(requestCaptor.capture());
        assertTrue(requestCaptor.getAllValues().get(1).previousToolResult()
                .contains("B001"));
        assertTrue(requestCaptor.getAllValues().get(2).previousToolResult()
                .contains("FLYAI_HOTEL_SEARCH"));
    }

    @Test
    void shouldBlockIdenticalRepeatedToolCallBeforeSecondExecution() {
        AmapPoiSearchToolRequest input =
                new AmapPoiSearchToolRequest("鼓浪屿", "厦门", 3);
        PlannerToolDecision decision = new PlannerToolDecision(
                PlannerActionType.CALL_TOOL,
                PlannerToolName.AMAP_POI_SEARCH, "需要地点坐标",
                input, null, null, null);
        when(plannerAgentService.decideNextStep(any(PlannerAgentRequest.class)))
                .thenReturn(decision, decision);
        when(plannerAgentService.executeDecision(decision)).thenReturn(
                new PlannerAgentStepResult(
                        PlannerActionType.CALL_TOOL,
                        PlannerToolName.AMAP_POI_SEARCH, "需要地点坐标",
                        input, poiResult("B001", "鼓浪屿"), null));

        PlannerWorkflowResponse response = service.run(7L, 12L, "查鼓浪屿");

        assertEquals(PlannerWorkflowStatus.REPEATED_TOOL_CALL_BLOCKED, response.status());
        assertEquals(1, response.toolCallCount());
        assertEquals(2, response.steps().size());
        verify(plannerAgentService, times(1)).executeDecision(decision);
    }

    @Test
    void shouldKeepSuccessfulFactsWhenLaterToolFails() {
        AmapPoiSearchToolRequest poiInput =
                new AmapPoiSearchToolRequest("鼓浪屿", "厦门", 3);
        FlyAiHotelSearchToolRequest hotelInput =
                new FlyAiHotelSearchToolRequest("厦门", null, 600, 5);
        PlannerToolDecision poiDecision = new PlannerToolDecision(
                PlannerActionType.CALL_TOOL,
                PlannerToolName.AMAP_POI_SEARCH, "先查地点",
                poiInput, null, null, null);
        PlannerToolDecision hotelDecision = new PlannerToolDecision(
                PlannerActionType.CALL_TOOL,
                PlannerToolName.FLYAI_HOTEL_SEARCH, "再查酒店",
                null, null, hotelInput, null);
        when(plannerAgentService.decideNextStep(any(PlannerAgentRequest.class)))
                .thenReturn(poiDecision, hotelDecision);
        when(plannerAgentService.executeDecision(poiDecision)).thenReturn(
                new PlannerAgentStepResult(
                        PlannerActionType.CALL_TOOL,
                        PlannerToolName.AMAP_POI_SEARCH, "先查地点",
                        poiInput, poiResult("B001", "鼓浪屿"), null));
        when(plannerAgentService.executeDecision(hotelDecision))
                .thenThrow(new RuntimeException("第三方服务失败"));

        PlannerWorkflowResponse response = service.run(7L, 12L, "查地点和酒店");

        assertEquals(PlannerWorkflowStatus.PARTIAL_FAILURE, response.status());
        assertEquals(1, response.toolCallCount());
        assertEquals(1, response.facts().poiResults().size());
        assertTrue(response.steps().get(1).errorMessage().contains("执行失败"));
    }

    @Test
    void shouldStopAfterFiveToolCalls() {
        List<PlannerToolDecision> decisions = new ArrayList<>();
        for (int index = 1; index <= PlannerWorkflowService.MAX_TOOL_CALLS; index++) {
            decisions.add(new PlannerToolDecision(
                    PlannerActionType.CALL_TOOL,
                    PlannerToolName.AMAP_POI_SEARCH,
                    "查询第" + index + "个地点",
                    new AmapPoiSearchToolRequest("地点" + index, "厦门", 1),
                    null,
                    null,
                    null
            ));
        }
        decisions.add(new PlannerToolDecision(
                PlannerActionType.CALL_TOOL,
                PlannerToolName.AMAP_POI_SEARCH,
                "查询第6个地点",
                new AmapPoiSearchToolRequest("地点6", "厦门", 1),
                null,
                null,
                null
        ));
        AtomicInteger decisionIndex = new AtomicInteger();
        when(plannerAgentService.decideNextStep(any(PlannerAgentRequest.class)))
                .thenAnswer(invocation -> decisions.get(decisionIndex.getAndIncrement()));
        when(plannerAgentService.executeDecision(any(PlannerToolDecision.class)))
                .thenAnswer(invocation -> {
                    PlannerToolDecision decision = invocation.getArgument(0);
                    return new PlannerAgentStepResult(
                            PlannerActionType.CALL_TOOL,
                            decision.tool(),
                            decision.reason(),
                            decision.poiSearch(),
                            poiResult("B" + decisionIndex.get(), decision.poiSearch().keyword()),
                            null
                    );
                });

        PlannerWorkflowResponse response = service.run(7L, 12L, "查询多个地点");

        assertEquals(PlannerWorkflowStatus.STEP_LIMIT_REACHED, response.status());
        assertEquals(5, response.toolCallCount());
        assertEquals(5, response.facts().poiResults().size());
        verify(plannerAgentService, times(5))
                .executeDecision(any(PlannerToolDecision.class));
    }

    @Test
    void shouldNotCallPlannerWhenPlanOwnershipCheckFails() {
        ResponseStatusException notFound =
                new ResponseStatusException(HttpStatus.NOT_FOUND, "旅行计划不存在");
        when(plannerContextService.build(7L, 12L)).thenThrow(notFound);

        ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
                () -> service.run(7L, 12L, "查询路线"));

        assertEquals(notFound, thrown);
        verifyNoInteractions(plannerAgentService);
    }

    private PlannerTravelContext emptyContext() {
        return new PlannerTravelContext(null, null, List.of(), List.of());
    }

    private AmapPoiSearchToolResult poiResult(String poiId, String name) {
        return new AmapPoiSearchToolResult(
                poiId,
                name,
                "厦门市思明区",
                new BigDecimal("118.067000"),
                new BigDecimal("24.447000"),
                "0592",
                null
        );
    }

    private AiTravelPlanResponse validFinalPlan() {
        AiTravelDay day = new AiTravelDay();
        day.setDayNumber(1);
        day.setItems(List.of());
        AiTravelPlanResponse plan = new AiTravelPlanResponse();
        plan.setDestination("厦门");
        plan.setTravelDays(1);
        plan.setDays(List.of(day));
        return plan;
    }
}
