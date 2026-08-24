package com.lanyu.xiaolanaitravel.ai.agent.planner;

import com.lanyu.xiaolanaitravel.ai.tool.amap.dto.AmapPoiSearchToolResult;
import com.lanyu.xiaolanaitravel.ai.tool.amap.dto.AmapTransitRouteToolResult;
import com.lanyu.xiaolanaitravel.ai.tool.flyai.dto.FlyAiHotelSearchToolResult;

import java.util.List;

/** Workflow 已经获得的真实外部事实，按 Tool 类型分别保存。 */
public record PlannerWorkflowFacts(
        List<AmapPoiSearchToolResult> poiResults,
        List<AmapTransitRouteToolResult> transitResults,
        List<FlyAiHotelSearchToolResult> hotelResults
) {
    public PlannerWorkflowFacts {
        poiResults = poiResults == null ? List.of() : List.copyOf(poiResults);
        transitResults = transitResults == null ? List.of() : List.copyOf(transitResults);
        hotelResults = hotelResults == null ? List.of() : List.copyOf(hotelResults);
    }
}
