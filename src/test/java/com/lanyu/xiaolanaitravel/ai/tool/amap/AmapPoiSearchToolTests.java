package com.lanyu.xiaolanaitravel.ai.tool.amap;

import com.lanyu.xiaolanaitravel.ai.tool.amap.dto.AmapPoiSearchToolRequest;
import com.lanyu.xiaolanaitravel.ai.tool.amap.dto.AmapPoiSearchToolResult;
import com.lanyu.xiaolanaitravel.amap.dto.AmapPoiItem;
import com.lanyu.xiaolanaitravel.amap.dto.AmapPoiPhoto;
import com.lanyu.xiaolanaitravel.amap.service.AmapPoiMatcher;
import com.lanyu.xiaolanaitravel.amap.service.AmapPoiSearchCache;
import com.lanyu.xiaolanaitravel.amap.service.AmapService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AmapPoiSearchToolTests {

    @Mock
    private AmapService amapService;

    private AmapPoiSearchTool tool;

    @BeforeEach
    void setUp() {
        tool = new AmapPoiSearchTool(
                amapService,
                new AmapPoiSearchCache(),
                new AmapPoiMatcher()
        );
    }

    @Test
    void shouldReturnStablePoiResult() {
        AmapPoiItem poi = createPoi();
        when(amapService.searchPois("鼓浪屿", "厦门", 3)).thenReturn(List.of(poi));

        AmapPoiSearchToolResult result = tool.execute(
                new AmapPoiSearchToolRequest(" 鼓浪屿 ", " 厦门 ", null)
        );

        assertEquals("B0FFG123", result.poiId());
        assertEquals("鼓浪屿", result.name());
        assertEquals(new BigDecimal("118.067000"), result.longitude());
        assertEquals(new BigDecimal("24.447000"), result.latitude());
        assertEquals("350200", result.cityCode());
        assertEquals("https://example.com/gulangyu.jpg", result.imageUrl());
        verify(amapService).searchPois("鼓浪屿", "厦门", 3);
    }

    @Test
    void shouldCapCandidateLimitAtFive() {
        AmapPoiItem poi = createPoi();
        when(amapService.searchPois("鼓浪屿", "厦门", 5)).thenReturn(List.of(poi));

        tool.execute(new AmapPoiSearchToolRequest("鼓浪屿", "厦门", 99));

        verify(amapService).searchPois("鼓浪屿", "厦门", 5);
    }

    @Test
    void shouldRejectBlankKeywordWithoutCallingAmap() {
        assertThrows(IllegalArgumentException.class, () -> tool.execute(
                new AmapPoiSearchToolRequest(" ", "厦门", 3)
        ));

        verify(amapService, never()).searchPois("", "厦门", 3);
    }

    @Test
    void shouldExposeSpringAiToolDefinition() {
        ToolCallback[] callbacks = ToolCallbacks.from(tool);

        assertEquals(1, callbacks.length);
        assertEquals("amapPoiSearch", callbacks[0].getToolDefinition().name());
        assertTrue(callbacks[0].getToolDefinition().description().contains("真实POI信息"));

        String inputSchema = callbacks[0].getToolDefinition().inputSchema();
        assertTrue(inputSchema.contains("keyword"));
        assertTrue(inputSchema.contains("city"));
        assertTrue(inputSchema.contains("limit"));
        assertFalse(inputSchema.contains("AmapPoiSearchToolRequest"));

        verify(amapService, never()).searchPois("鼓浪屿", "厦门", 3);
    }

    @Test
    void shouldExecuteSpringAiToolCallbackWithMockedAmap() {
        AmapPoiItem poi = createPoi();
        when(amapService.searchPois("鼓浪屿", "厦门", 3)).thenReturn(List.of(poi));
        ToolCallback callback = ToolCallbacks.from(tool)[0];

        String result = callback.call("""
                {"keyword":"鼓浪屿","city":"厦门","limit":3}
                """);

        assertTrue(result.contains("B0FFG123"));
        assertTrue(result.contains("鼓浪屿"));
        verify(amapService).searchPois("鼓浪屿", "厦门", 3);
    }

    private AmapPoiItem createPoi() {
        AmapPoiPhoto photo = new AmapPoiPhoto();
        photo.setUrl("https://example.com/gulangyu.jpg");

        AmapPoiItem poi = new AmapPoiItem();
        poi.setId("B0FFG123");
        poi.setName("鼓浪屿");
        poi.setAddress("厦门市思明区鼓浪屿");
        poi.setLocation("118.067000,24.447000");
        poi.setCityname("厦门市");
        poi.setCitycode("350200");
        poi.setPhotos(List.of(photo));
        return poi;
    }
}
