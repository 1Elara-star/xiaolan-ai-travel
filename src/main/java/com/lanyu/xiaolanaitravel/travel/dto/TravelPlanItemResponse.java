package com.lanyu.xiaolanaitravel.travel.dto;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.LocalDateTime;

public record TravelPlanItemResponse(
        Long id,
        Long planId,
        Integer dayNumber,
        Integer itemOrder,
        String itemType,
        Long attractionId,
        String placeName,
        String address,
        BigDecimal longitude,
        BigDecimal latitude,
        String cityCode,
        LocalTime startTime,
        LocalTime endTime,
        Integer endDayOffset,
        String transportMode,
        Integer distanceFromPrev,
        Integer travelTimeFromPrev,
        Integer straightLineDistanceFromPrev,
        String description,
        String imageUrl,
        LocalDateTime createTime,
        LocalDateTime updateTime) {
}
