package com.lanyu.xiaolanaitravel.explore.dto;

import java.util.List;

public record CityExploreResponse(
        String slug,
        String name,
        String slogan,
        String description,
        String heroImage,
        String bestSeason,
        String recommendedDays,
        List<String> categories,
        List<AttractionResponse> attractions) {
}
