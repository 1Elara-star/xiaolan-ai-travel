package com.lanyu.xiaolanaitravel.ai.agent.planner;

import com.lanyu.xiaolanaitravel.ai.tool.amap.dto.AmapPoiSearchToolRequest;
import com.lanyu.xiaolanaitravel.ai.tool.amap.dto.AmapTransitRouteToolRequest;
import com.lanyu.xiaolanaitravel.ai.tool.flyai.dto.FlyAiHotelSearchToolRequest;

/** DeepSeek Planner 返回的单步结构化决策。 */
public record PlannerToolDecision(
        PlannerToolName tool,
        String reason,
        AmapPoiSearchToolRequest poiSearch,
        AmapTransitRouteToolRequest transitRoute,
        FlyAiHotelSearchToolRequest hotelSearch
) {
}
