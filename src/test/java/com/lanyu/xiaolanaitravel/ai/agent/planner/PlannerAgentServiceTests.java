package com.lanyu.xiaolanaitravel.ai.agent.planner;

import com.lanyu.xiaolanaitravel.ai.service.DeepSeekService;
import com.lanyu.xiaolanaitravel.ai.tool.amap.AmapPoiSearchTool;
import com.lanyu.xiaolanaitravel.ai.tool.amap.AmapTransitRouteTool;
import com.lanyu.xiaolanaitravel.ai.tool.amap.dto.AmapPoiSearchToolRequest;
import com.lanyu.xiaolanaitravel.ai.tool.amap.dto.AmapPoiSearchToolResult;
import com.lanyu.xiaolanaitravel.ai.tool.amap.dto.AmapTransitRouteToolRequest;
import com.lanyu.xiaolanaitravel.ai.tool.amap.dto.AmapTransitRouteToolResult;
import com.lanyu.xiaolanaitravel.ai.tool.flyai.FlyAiHotelSearchTool;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlannerAgentServiceTests {

    @Mock
    private DeepSeekService deepSeekService;
    @Mock
    private AmapPoiSearchTool poiSearchTool;
    @Mock
    private AmapTransitRouteTool transitRouteTool;
    @Mock
    private FlyAiHotelSearchTool hotelSearchTool;

    private PlannerAgentService service;

    @BeforeEach
    void setUp() {
        service = new PlannerAgentService(
                deepSeekService, poiSearchTool, transitRouteTool, hotelSearchTool);
    }

    @Test
    void shouldExecuteOnlySelectedPoiTool() {
        AmapPoiSearchToolRequest toolRequest =
                new AmapPoiSearchToolRequest("鼓浪屿", "厦门", 3);
        AmapPoiSearchToolResult toolResult = new AmapPoiSearchToolResult(
                "B001", "鼓浪屿", "厦门市思明区",
                new BigDecimal("118.067000"), new BigDecimal("24.447000"),
                "0592", null);
        mockDecision(new PlannerToolDecision(
                PlannerToolName.AMAP_POI_SEARCH, "需要先取得真实坐标",
                toolRequest, null, null));
        when(poiSearchTool.execute(toolRequest)).thenReturn(toolResult);

        PlannerAgentStepResult result = service.executeNextStep(request());

        assertEquals(PlannerToolName.AMAP_POI_SEARCH, result.tool());
        assertSame(toolResult, result.toolResult());
        verify(poiSearchTool).execute(toolRequest);
        verifyNoInteractions(transitRouteTool, hotelSearchTool);
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
        mockDecision(new PlannerToolDecision(
                PlannerToolName.AMAP_TRANSIT_ROUTE, "已有坐标，需要核对末班车",
                null, toolRequest, null));
        when(transitRouteTool.execute(toolRequest)).thenReturn(toolResult);

        PlannerAgentStepResult result = service.executeNextStep(request());

        assertSame(toolResult, result.toolResult());
        verify(transitRouteTool).execute(toolRequest);
        verifyNoInteractions(poiSearchTool, hotelSearchTool);
    }

    @Test
    void shouldExecuteOnlySelectedHotelTool() {
        FlyAiHotelSearchToolRequest toolRequest =
                new FlyAiHotelSearchToolRequest("成都", "春熙路", 600, 5);
        FlyAiHotelSearchToolResult toolResult =
                new FlyAiHotelSearchToolResult(List.of(), 0);
        mockDecision(new PlannerToolDecision(
                PlannerToolName.FLYAI_HOTEL_SEARCH, "需要真实酒店候选",
                null, null, toolRequest));
        when(hotelSearchTool.execute(toolRequest)).thenReturn(toolResult);

        PlannerAgentStepResult result = service.executeNextStep(request());

        assertSame(toolResult, result.toolResult());
        verify(hotelSearchTool).execute(toolRequest);
        verifyNoInteractions(poiSearchTool, transitRouteTool);
    }

    @Test
    void shouldNotExecuteAnyToolWhenPlannerSelectsNone() {
        mockDecision(new PlannerToolDecision(
                PlannerToolName.NONE, "当前问题不需要外部事实", null, null, null));

        PlannerAgentStepResult result = service.executeNextStep(request());

        assertEquals(PlannerToolName.NONE, result.tool());
        assertNull(result.toolResult());
        verifyNoInteractions(poiSearchTool, transitRouteTool, hotelSearchTool);
    }

    @Test
    void shouldRejectDecisionContainingParametersForMultipleTools() {
        AmapPoiSearchToolRequest poiRequest =
                new AmapPoiSearchToolRequest("鼓浪屿", "厦门", 3);
        FlyAiHotelSearchToolRequest hotelRequest =
                new FlyAiHotelSearchToolRequest("厦门", null, null, 5);
        mockDecision(new PlannerToolDecision(
                PlannerToolName.AMAP_POI_SEARCH, "错误的多工具参数",
                poiRequest, null, hotelRequest));

        assertThrows(ResponseStatusException.class,
                () -> service.executeNextStep(request()));
        verify(poiSearchTool, never()).execute(poiRequest);
        verifyNoInteractions(transitRouteTool, hotelSearchTool);
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
}
