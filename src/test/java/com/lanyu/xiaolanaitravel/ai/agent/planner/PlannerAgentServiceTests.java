package com.lanyu.xiaolanaitravel.ai.agent.planner;

import com.lanyu.xiaolanaitravel.ai.dto.AiTravelDay;
import com.lanyu.xiaolanaitravel.ai.dto.AiTravelPlanResponse;
import com.lanyu.xiaolanaitravel.ai.service.DeepSeekService;
import com.lanyu.xiaolanaitravel.ai.tool.amap.dto.AmapPoiSearchToolRequest;
import com.lanyu.xiaolanaitravel.ai.tool.amap.dto.AmapPoiSearchToolResult;
import com.lanyu.xiaolanaitravel.ai.tool.amap.dto.AmapTransitRouteToolRequest;
import com.lanyu.xiaolanaitravel.ai.tool.amap.dto.AmapTransitRouteToolResult;
import com.lanyu.xiaolanaitravel.ai.tool.flyai.dto.FlyAiHotelSearchToolRequest;
import com.lanyu.xiaolanaitravel.ai.tool.flyai.dto.FlyAiHotelSearchToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlannerAgentServiceTests {

    @Mock
    private DeepSeekService deepSeekService;
    @Mock
    private PlannerToolExecutor plannerToolExecutor;

    private PlannerAgentService service;

    @BeforeEach
    void setUp() {
        service = new PlannerAgentService(deepSeekService, plannerToolExecutor);
    }

    @Test
    void shouldExecuteOnlySelectedPoiTool() {
        AmapPoiSearchToolRequest toolRequest =
                new AmapPoiSearchToolRequest("鼓浪屿", "厦门", 3);
        AmapPoiSearchToolResult toolResult = new AmapPoiSearchToolResult(
                "B001", "鼓浪屿", "厦门市思明区",
                new BigDecimal("118.067000"), new BigDecimal("24.447000"),
                "0592", null);
        PlannerToolDecision decision = new PlannerToolDecision(
                PlannerActionType.CALL_TOOL,
                PlannerToolName.AMAP_POI_SEARCH, "需要先取得真实坐标",
                toolRequest, null, null, null);
        mockDecision(decision);
        when(plannerToolExecutor.execute(decision)).thenReturn(toolResult);

        PlannerAgentStepResult result = service.executeNextStep(request());

        assertEquals(PlannerToolName.AMAP_POI_SEARCH, result.tool());
        assertEquals(PlannerActionType.CALL_TOOL, result.action());
        assertSame(toolResult, result.toolResult());
        verify(plannerToolExecutor).execute(decision);
    }

    @Test
    void shouldExecuteOnlySelectedTransitTool() {
        AmapTransitRouteToolRequest toolRequest = new AmapTransitRouteToolRequest(
                new BigDecimal("118.080000"), new BigDecimal("24.450000"),
                new BigDecimal("118.120000"), new BigDecimal("24.520000"),
                "0592", "0592", "2026-08-23", "22:30", true);
        AmapTransitRouteToolResult toolResult = new AmapTransitRouteToolResult(
                2584, 2100, 35, false,
                "2026-08-23", "22:30", List.of());
        PlannerToolDecision decision = new PlannerToolDecision(
                PlannerActionType.CALL_TOOL,
                PlannerToolName.AMAP_TRANSIT_ROUTE, "已有坐标，需要核对末班车",
                null, toolRequest, null, null);
        mockDecision(decision);
        when(plannerToolExecutor.execute(decision)).thenReturn(toolResult);

        PlannerAgentStepResult result = service.executeNextStep(request());

        assertSame(toolResult, result.toolResult());
        verify(plannerToolExecutor).execute(decision);
    }

    @Test
    void shouldExecuteOnlySelectedHotelTool() {
        FlyAiHotelSearchToolRequest toolRequest =
                new FlyAiHotelSearchToolRequest("成都", "春熙路", 600, 5);
        FlyAiHotelSearchToolResult toolResult =
                new FlyAiHotelSearchToolResult(List.of(), 0);
        PlannerToolDecision decision = new PlannerToolDecision(
                PlannerActionType.CALL_TOOL,
                PlannerToolName.FLYAI_HOTEL_SEARCH, "需要真实酒店候选",
                null, null, toolRequest, null);
        mockDecision(decision);
        when(plannerToolExecutor.execute(decision)).thenReturn(toolResult);

        PlannerAgentStepResult result = service.executeNextStep(request());

        assertSame(toolResult, result.toolResult());
        verify(plannerToolExecutor).execute(decision);
    }

    @Test
    void shouldReturnFinalDraftWithoutExecutingTool() {
        AiTravelPlanResponse finalPlan = validFinalPlan();
        PlannerToolDecision decision = new PlannerToolDecision(
                PlannerActionType.FINAL_DRAFT,
                PlannerToolName.NONE, "事实已经足够",
                null, null, null, finalPlan);
        mockDecision(decision);

        PlannerAgentStepResult result = service.executeNextStep(request());

        assertEquals(PlannerToolName.NONE, result.tool());
        assertEquals(PlannerActionType.FINAL_DRAFT, result.action());
        assertNull(result.toolResult());
        assertSame(finalPlan, result.finalPlan());
        verify(plannerToolExecutor, never()).execute(decision);
    }

    @Test
    void shouldRejectDecisionContainingParametersForMultipleTools() {
        AmapPoiSearchToolRequest poiRequest =
                new AmapPoiSearchToolRequest("鼓浪屿", "厦门", 3);
        FlyAiHotelSearchToolRequest hotelRequest =
                new FlyAiHotelSearchToolRequest("厦门", null, null, 5);
        PlannerToolDecision decision = new PlannerToolDecision(
                PlannerActionType.CALL_TOOL,
                PlannerToolName.AMAP_POI_SEARCH, "错误的多工具参数",
                poiRequest, null, hotelRequest, null);
        mockDecision(decision);

        assertThrows(ResponseStatusException.class,
                () -> service.executeNextStep(request()));
        verify(plannerToolExecutor, never()).execute(decision);
    }

    private void mockDecision(PlannerToolDecision decision) {
        when(deepSeekService.generateStructuredResponse(
                anyString(), anyString(), eq(PlannerToolDecision.class)))
                .thenReturn(decision);
    }

    private PlannerAgentRequest request() {
        return new PlannerAgentRequest(
                "演唱会结束后还能坐地铁回酒店吗？",
                "已经拥有场馆和酒店的真实坐标与城市编码",
                null);
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
