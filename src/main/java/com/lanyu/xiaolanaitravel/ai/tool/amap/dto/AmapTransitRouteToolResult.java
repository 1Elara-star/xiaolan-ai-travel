package com.lanyu.xiaolanaitravel.ai.tool.amap.dto;

import java.util.List;

/**
 * 高德公交路线 Tool 输出。
 *
 * <p>首末班时间可能为空，表示高德本次没有返回，AI 不得自行猜测。</p>
 */
public record AmapTransitRouteToolResult(
        int distanceMeters,
        int durationSeconds,
        int durationMinutes,
        boolean nightRoute,
        String departureDate,
        String departureTime,
        List<AmapTransitLineToolResult> lines
) {

    public AmapTransitRouteToolResult {
        lines = lines == null ? List.of() : List.copyOf(lines);
    }
}
