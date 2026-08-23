package com.lanyu.xiaolanaitravel.ai.tool.amap.dto;

/** 返回给 AI 层的单段公交或地铁线路信息。 */
public record AmapTransitLineToolResult(
        String lineName,
        String lineType,
        String departureStopName,
        String arrivalStopName,
        Integer distanceMeters,
        Integer durationSeconds,
        String lineStartTime,
        String lineEndTime,
        String boardingStationStartTime,
        String boardingStationEndTime
) {
}
