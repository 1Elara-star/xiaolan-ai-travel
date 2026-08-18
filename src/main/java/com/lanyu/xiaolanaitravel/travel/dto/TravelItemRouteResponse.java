package com.lanyu.xiaolanaitravel.travel.dto;

import com.lanyu.xiaolanaitravel.amap.dto.AmapTravelMode;

import java.time.LocalDateTime;

/** 相邻行程节点之间的高德路线摘要。 */
public record TravelItemRouteResponse(
        Long fromItemId,
        Long toItemId,
        AmapTravelMode mode,
        Integer distanceMeters,
        Integer durationSeconds,
        Integer durationMinutes,
        String source,
        LocalDateTime queriedAt) {
}
