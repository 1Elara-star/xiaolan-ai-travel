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
        LocalTime startTime,
        LocalTime endTime,
        String transportMode,
        Integer distanceFromPrev,
        Integer travelTimeFromPrev,
        String description,
        LocalDateTime createTime,
        LocalDateTime updateTime) {
}
