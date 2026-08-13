package com.lanyu.xiaolanaitravel.travel.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TravelPlanResponse(
        Long id,
        String title,
        String departureCity,
        String destination,
        LocalDate startDate,
        LocalDate endDate,
        Integer travelDays,
        Integer peopleCount,
        String companionType,
        BigDecimal budget,
        String tripType,
        String tripPreferences,
        String specialRequirements,
        String tripStatus,
        LocalDateTime createTime,
        LocalDateTime updateTime) {
}
