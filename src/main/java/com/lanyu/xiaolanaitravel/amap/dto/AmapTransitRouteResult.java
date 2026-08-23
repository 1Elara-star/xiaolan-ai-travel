package com.lanyu.xiaolanaitravel.amap.dto;

import java.util.List;

/** 包含线路与首末班信息的高德公交路线结果。 */
public record AmapTransitRouteResult(
        int distanceMeters,
        int durationSeconds,
        boolean nightRoute,
        List<AmapTransitLineResult> lines
) {

    public AmapTransitRouteResult {
        lines = lines == null ? List.of() : List.copyOf(lines);
    }
}
