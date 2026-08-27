package com.lanyu.xiaolanaitravel.ai.agent.planner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lanyu.xiaolanaitravel.ai.dto.FlyAiHotelData;
import com.lanyu.xiaolanaitravel.ai.dto.FlyAiHotelItem;
import com.lanyu.xiaolanaitravel.ai.dto.FlyAiHotelResponse;
import com.lanyu.xiaolanaitravel.ai.service.FlyAiService;
import com.lanyu.xiaolanaitravel.ai.tool.amap.AmapPoiSearchTool;
import com.lanyu.xiaolanaitravel.ai.tool.amap.AmapTransitRouteTool;
import com.lanyu.xiaolanaitravel.ai.tool.amap.dto.AmapPoiSearchToolRequest;
import com.lanyu.xiaolanaitravel.ai.tool.amap.dto.AmapPoiSearchToolResult;
import com.lanyu.xiaolanaitravel.ai.tool.amap.dto.AmapTransitRouteToolRequest;
import com.lanyu.xiaolanaitravel.ai.tool.amap.dto.AmapTransitRouteToolResult;
import com.lanyu.xiaolanaitravel.ai.tool.flyai.FlyAiHotelSearchTool;
import com.lanyu.xiaolanaitravel.ai.tool.flyai.dto.FlyAiHotelSearchToolRequest;
import com.lanyu.xiaolanaitravel.ai.tool.flyai.dto.FlyAiHotelSearchToolResult;
import com.lanyu.xiaolanaitravel.amap.dto.AmapPoiItem;
import com.lanyu.xiaolanaitravel.amap.dto.AmapTransitLineResult;
import com.lanyu.xiaolanaitravel.amap.dto.AmapTransitRouteResult;
import com.lanyu.xiaolanaitravel.amap.service.AmapPoiMatcher;
import com.lanyu.xiaolanaitravel.amap.service.AmapPoiSearchCache;
import com.lanyu.xiaolanaitravel.amap.service.AmapService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlannerToolExecutorTests {

    @Mock
    private AmapService amapService;
    @Mock
    private FlyAiService flyAiService;

    private PlannerToolExecutor executor;

    @BeforeEach
    void setUp() {
        AmapPoiSearchTool poiSearchTool = new AmapPoiSearchTool(
                amapService,
                new AmapPoiSearchCache(),
                new AmapPoiMatcher()
        );
        AmapTransitRouteTool transitRouteTool = new AmapTransitRouteTool(amapService);
        FlyAiHotelSearchTool hotelSearchTool = new FlyAiHotelSearchTool(flyAiService);
        executor = new PlannerToolExecutor(
                new ObjectMapper(),
                poiSearchTool,
                transitRouteTool,
                hotelSearchTool
        );
    }

    @Test
    void shouldExecutePoiThroughSpringAiToolCallback() {
        AmapPoiItem poi = new AmapPoiItem();
        poi.setId("B001");
        poi.setName("鼓浪屿");
        poi.setAddress("厦门市思明区");
        poi.setLocation("118.067000,24.447000");
        poi.setCitycode("0592");
        when(amapService.searchPois("鼓浪屿", "厦门", 3)).thenReturn(List.of(poi));

        Object result = executor.execute(new PlannerToolDecision(
                PlannerActionType.CALL_TOOL,
                PlannerToolName.AMAP_POI_SEARCH,
                "查询真实地点",
                new AmapPoiSearchToolRequest("鼓浪屿", "厦门", 3),
                null,
                null,
                null
        ));

        AmapPoiSearchToolResult poiResult = assertInstanceOf(
                AmapPoiSearchToolResult.class, result);
        assertEquals("B001", poiResult.poiId());
        assertEquals("鼓浪屿", poiResult.name());
        verify(amapService).searchPois("鼓浪屿", "厦门", 3);
        verifyNoInteractions(flyAiService);
    }

    @Test
    void shouldExecuteTransitThroughSpringAiToolCallback() {
        AmapTransitLineResult line = new AmapTransitLineResult(
                "厦门地铁1号线", "地铁线路", "镇海路", "高崎",
                22000, 1800, "06:00", "23:30", "06:15", "23:05");
        when(amapService.calculateTransitRoute(
                any(BigDecimal.class), any(BigDecimal.class),
                any(BigDecimal.class), any(BigDecimal.class),
                eq("0592"), eq("0592"),
                eq(LocalDate.of(2026, 8, 23)), eq(LocalTime.of(22, 30)), eq(true)))
                .thenReturn(new AmapTransitRouteResult(2584, 2100, false, List.of(line)));

        Object result = executor.execute(new PlannerToolDecision(
                PlannerActionType.CALL_TOOL,
                PlannerToolName.AMAP_TRANSIT_ROUTE,
                "查询真实公交路线",
                null,
                new AmapTransitRouteToolRequest(
                        new BigDecimal("118.080000"), new BigDecimal("24.450000"),
                        new BigDecimal("118.120000"), new BigDecimal("24.520000"),
                        "0592", "0592", "2026-08-23", "22:30", true),
                null,
                null
        ));

        AmapTransitRouteToolResult transitResult = assertInstanceOf(
                AmapTransitRouteToolResult.class, result);
        assertEquals(2584, transitResult.distanceMeters());
        assertEquals("厦门地铁1号线", transitResult.lines().get(0).lineName());
        verifyNoInteractions(flyAiService);
    }

    @Test
    void shouldExecuteHotelThroughSpringAiToolCallback() {
        FlyAiHotelItem hotel = new FlyAiHotelItem();
        hotel.setName("酒店A");
        hotel.setPrice("399");
        FlyAiHotelData data = new FlyAiHotelData();
        data.setItemList(List.of(hotel));
        FlyAiHotelResponse response = new FlyAiHotelResponse();
        response.setData(data);
        when(flyAiService.searchHotels("成都", "春熙路", 600)).thenReturn(response);

        Object result = executor.execute(new PlannerToolDecision(
                PlannerActionType.CALL_TOOL,
                PlannerToolName.FLYAI_HOTEL_SEARCH,
                "查询真实酒店",
                null,
                null,
                new FlyAiHotelSearchToolRequest("成都", "春熙路", 600, 5),
                null
        ));

        FlyAiHotelSearchToolResult hotelResult = assertInstanceOf(
                FlyAiHotelSearchToolResult.class, result);
        assertEquals(1, hotelResult.count());
        assertEquals("酒店A", hotelResult.hotels().get(0).name());
        verify(flyAiService).searchHotels("成都", "春熙路", 600);
    }

    @Test
    void shouldRejectFinalDraftBecauseItIsNotAToolCall() {
        PlannerToolDecision decision = new PlannerToolDecision(
                PlannerActionType.FINAL_DRAFT,
                PlannerToolName.NONE,
                "不需要外部数据",
                null,
                null,
                null,
                null
        );

        assertThrows(IllegalArgumentException.class, () -> executor.execute(decision));
        verifyNoInteractions(amapService, flyAiService);
    }
}
