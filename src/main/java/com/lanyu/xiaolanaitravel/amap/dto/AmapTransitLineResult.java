package com.lanyu.xiaolanaitravel.amap.dto;

/** 项目内部使用的单段公交或地铁线路结果。 */
public record AmapTransitLineResult(
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
