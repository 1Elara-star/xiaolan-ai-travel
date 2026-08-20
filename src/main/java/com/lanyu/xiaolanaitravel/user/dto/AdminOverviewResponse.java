package com.lanyu.xiaolanaitravel.user.dto;

public record AdminOverviewResponse(
        long userCount,
        long adminCount,
        long planCount,
        long attractionCount,
        long favoriteCount,
        long memoryCount) {
}
