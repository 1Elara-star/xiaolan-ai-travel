package com.lanyu.xiaolanaitravel.user.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminPlanResponse(
        Long id,
        String title,
        Long userId,
        String username,
        String destination,
        LocalDate startDate,
        LocalDate endDate,
        Integer travelDays,
        String tripStatus,
        LocalDateTime createTime) {
}
