package com.lanyu.xiaolanaitravel.ai.tool.amap;

import com.lanyu.xiaolanaitravel.ai.tool.amap.dto.AmapTransitRouteToolRequest;
import com.lanyu.xiaolanaitravel.ai.tool.amap.dto.AmapTransitRouteToolResult;
import com.lanyu.xiaolanaitravel.amap.dto.AmapTransitLineResult;
import com.lanyu.xiaolanaitravel.amap.dto.AmapTransitRouteResult;
import com.lanyu.xiaolanaitravel.amap.service.AmapService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AmapTransitRouteToolTests {

    @Mock
    private AmapService amapService;

    private AmapTransitRouteTool tool;

    @BeforeEach
    void setUp() {
        tool = new AmapTransitRouteTool(amapService);
    }

    @Test
    void shouldReturnStationAndLastTrainTimes() {
        AmapTransitLineResult line = line("06:00", "23:30", "06:15", "23:05");
        whenRoute(false, line);

        AmapTransitRouteToolResult result = tool.execute(validRequest(false));

        assertEquals(2584, result.distanceMeters());
        assertEquals(35, result.durationMinutes());
        assertEquals("厦门地铁1号线", result.lines().get(0).lineName());
        assertEquals("镇海路", result.lines().get(0).departureStopName());
        assertEquals("高崎", result.lines().get(0).arrivalStopName());
        assertEquals("23:30", result.lines().get(0).lineEndTime());
        assertEquals("23:05", result.lines().get(0).boardingStationEndTime());
    }

    @Test
    void shouldExposeNightRouteFlag() {
        whenRoute(true, line("06:00", "23:30", "06:15", "23:05"));

        AmapTransitRouteToolResult result = tool.execute(validRequest(true));

        assertTrue(result.nightRoute());
        verify(amapService).calculateTransitRoute(
                new BigDecimal("118.080000"), new BigDecimal("24.450000"),
                new BigDecimal("118.120000"), new BigDecimal("24.520000"),
                "0592", "0592",
                LocalDate.of(2026, 8, 23), LocalTime.of(22, 30), true);
    }

    @Test
    void shouldKeepMissingScheduleFieldsAsNull() {
        whenRoute(false, line(null, null, null, null));

        AmapTransitRouteToolResult result = tool.execute(validRequest(false));

        assertNull(result.lines().get(0).lineStartTime());
        assertNull(result.lines().get(0).lineEndTime());
        assertNull(result.lines().get(0).boardingStationStartTime());
        assertNull(result.lines().get(0).boardingStationEndTime());
    }

    @Test
    void shouldRejectInvalidTimeWithoutCallingAmap() {
        AmapTransitRouteToolRequest request = new AmapTransitRouteToolRequest(
                new BigDecimal("118.080000"), new BigDecimal("24.450000"),
                new BigDecimal("118.120000"), new BigDecimal("24.520000"),
                "0592", "0592", "2026-08-23", "25:30", true);

        assertThrows(IllegalArgumentException.class, () -> tool.execute(request));
        verify(amapService, never()).calculateTransitRoute(
                new BigDecimal("118.080000"), new BigDecimal("24.450000"),
                new BigDecimal("118.120000"), new BigDecimal("24.520000"),
                "0592", "0592",
                LocalDate.of(2026, 8, 23), null, true);
    }

    @Test
    void shouldExposeSpringAiTransitToolDefinition() {
        ToolCallback[] callbacks = ToolCallbacks.from(tool);

        assertEquals(1, callbacks.length);
        assertEquals("amapTransitRoute", callbacks[0].getToolDefinition().name());
        assertTrue(callbacks[0].getToolDefinition().description().contains("公交或地铁路线"));

        String inputSchema = callbacks[0].getToolDefinition().inputSchema();
        assertTrue(inputSchema.contains("originLongitude"));
        assertTrue(inputSchema.contains("destinationLatitude"));
        assertTrue(inputSchema.contains("departureTime"));
        assertFalse(inputSchema.contains("AmapTransitRouteToolRequest"));
    }

    @Test
    void shouldExecuteSpringAiTransitToolCallbackWithMockedAmap() {
        AmapTransitLineResult transitLine = line("06:00", "23:30", "06:15", "23:05");
        when(amapService.calculateTransitRoute(
                decimalEqualTo("118.080000"), decimalEqualTo("24.450000"),
                decimalEqualTo("118.120000"), decimalEqualTo("24.520000"),
                eq("0592"), eq("0592"),
                eq(LocalDate.of(2026, 8, 23)), eq(LocalTime.of(22, 30)), eq(false)))
                .thenReturn(new AmapTransitRouteResult(2584, 2100, false, List.of(transitLine)));
        ToolCallback callback = ToolCallbacks.from(tool)[0];

        String result = callback.call("""
                {
                  "originLongitude": 118.080000,
                  "originLatitude": 24.450000,
                  "destinationLongitude": 118.120000,
                  "destinationLatitude": 24.520000,
                  "originCityCode": "0592",
                  "destinationCityCode": "0592",
                  "departureDate": "2026-08-23",
                  "departureTime": "22:30",
                  "considerNightBus": false
                }
                """);

        assertTrue(result.contains("2584"));
        assertTrue(result.contains("厦门地铁1号线"));
        verify(amapService).calculateTransitRoute(
                decimalEqualTo("118.080000"), decimalEqualTo("24.450000"),
                decimalEqualTo("118.120000"), decimalEqualTo("24.520000"),
                eq("0592"), eq("0592"),
                eq(LocalDate.of(2026, 8, 23)), eq(LocalTime.of(22, 30)), eq(false));
    }

    private BigDecimal decimalEqualTo(String expected) {
        BigDecimal expectedValue = new BigDecimal(expected);
        return argThat(actual -> actual != null && actual.compareTo(expectedValue) == 0);
    }

    private void whenRoute(boolean nightRoute, AmapTransitLineResult line) {
        when(amapService.calculateTransitRoute(
                new BigDecimal("118.080000"), new BigDecimal("24.450000"),
                new BigDecimal("118.120000"), new BigDecimal("24.520000"),
                "0592", "0592",
                LocalDate.of(2026, 8, 23), LocalTime.of(22, 30), nightRoute))
                .thenReturn(new AmapTransitRouteResult(2584, 2100, nightRoute, List.of(line)));
    }

    private AmapTransitRouteToolRequest validRequest(boolean considerNightBus) {
        return new AmapTransitRouteToolRequest(
                new BigDecimal("118.080000"), new BigDecimal("24.450000"),
                new BigDecimal("118.120000"), new BigDecimal("24.520000"),
                "0592", "0592", "2026-08-23", "22:30", considerNightBus);
    }

    private AmapTransitLineResult line(
            String startTime,
            String endTime,
            String stationStartTime,
            String stationEndTime) {
        return new AmapTransitLineResult(
                "厦门地铁1号线", "地铁线路", "镇海路", "高崎",
                22000, 1800, startTime, endTime, stationStartTime, stationEndTime);
    }
}
