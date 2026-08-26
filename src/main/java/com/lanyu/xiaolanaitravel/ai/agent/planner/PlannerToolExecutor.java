package com.lanyu.xiaolanaitravel.ai.agent.planner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lanyu.xiaolanaitravel.ai.tool.amap.AmapPoiSearchTool;
import com.lanyu.xiaolanaitravel.ai.tool.amap.AmapTransitRouteTool;
import com.lanyu.xiaolanaitravel.ai.tool.amap.dto.AmapPoiSearchToolResult;
import com.lanyu.xiaolanaitravel.ai.tool.amap.dto.AmapTransitRouteToolResult;
import com.lanyu.xiaolanaitravel.ai.tool.flyai.FlyAiHotelSearchTool;
import com.lanyu.xiaolanaitravel.ai.tool.flyai.dto.FlyAiHotelSearchToolResult;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/**
 * Planner 的受控 Spring AI Tool 执行器。
 *
 * <p>这里只注册 Planner 白名单中的三个 Tool。Planner 先生成结构化决策，
 * 本执行器再调用对应的 Spring AI {@link ToolCallback}；它不会让模型自由选择
 * 未开放能力，也不负责循环调用、RAG、Repair 或行程保存。</p>
 */
@Component
public class PlannerToolExecutor {

    private final ObjectMapper objectMapper;
    private final ToolCallback poiSearchCallback;
    private final ToolCallback transitRouteCallback;
    private final ToolCallback hotelSearchCallback;

    public PlannerToolExecutor(
            ObjectMapper objectMapper,
            AmapPoiSearchTool amapPoiSearchTool,
            AmapTransitRouteTool amapTransitRouteTool,
            FlyAiHotelSearchTool flyAiHotelSearchTool) {
        this.objectMapper = objectMapper;
        this.poiSearchCallback = getSingleCallback(amapPoiSearchTool, "amapPoiSearch");
        this.transitRouteCallback = getSingleCallback(amapTransitRouteTool, "amapTransitRoute");
        this.hotelSearchCallback = getSingleCallback(flyAiHotelSearchTool, "flyAiHotelSearch");
    }

    /** 执行一项已经由 PlannerAgentService 校验过的白名单决策。 */
    public Object execute(PlannerToolDecision decision) {
        if (decision == null || decision.tool() == null) {
            throw new IllegalArgumentException("Planner Tool 决策不能为空");
        }

        return switch (decision.tool()) {
            case NONE -> null;
            case AMAP_POI_SEARCH -> call(
                    poiSearchCallback,
                    decision.poiSearch(),
                    AmapPoiSearchToolResult.class
            );
            case AMAP_TRANSIT_ROUTE -> call(
                    transitRouteCallback,
                    decision.transitRoute(),
                    AmapTransitRouteToolResult.class
            );
            case FLYAI_HOTEL_SEARCH -> call(
                    hotelSearchCallback,
                    decision.hotelSearch(),
                    FlyAiHotelSearchToolResult.class
            );
        };
    }

    private Object call(ToolCallback callback, Object input, Class<?> resultType) {
        if (input == null) {
            throw new IllegalArgumentException("Planner Tool 参数不能为空");
        }

        try {
            String inputJson = objectMapper.writeValueAsString(input);
            String resultJson = callback.call(inputJson);
            return objectMapper.readValue(resultJson, resultType);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Spring AI Tool 数据转换失败", exception);
        }
    }

    private ToolCallback getSingleCallback(Object tool, String expectedName) {
        ToolCallback[] callbacks = ToolCallbacks.from(tool);
        if (callbacks.length != 1
                || !expectedName.equals(callbacks[0].getToolDefinition().name())) {
            throw new IllegalStateException("Spring AI Tool 注册不正确: " + expectedName);
        }
        return callbacks[0];
    }
}
