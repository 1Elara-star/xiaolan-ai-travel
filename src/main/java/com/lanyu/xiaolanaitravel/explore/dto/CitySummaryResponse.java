package com.lanyu.xiaolanaitravel.explore.dto;

public record CitySummaryResponse(
        String slug,
        String name,
        String slogan,
        String description,
        String heroImage,
        String bestSeason,
        String recommendedDays,
        long attractionCount) {
}
