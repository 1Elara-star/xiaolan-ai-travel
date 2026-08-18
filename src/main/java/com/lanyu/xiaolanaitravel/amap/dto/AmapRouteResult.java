package com.lanyu.xiaolanaitravel.amap.dto;

/** 项目内部使用的路线结果，不直接暴露高德原始响应。 */
public record AmapRouteResult(
        AmapTravelMode mode,
        int distanceMeters,
        int durationSeconds) {
}
