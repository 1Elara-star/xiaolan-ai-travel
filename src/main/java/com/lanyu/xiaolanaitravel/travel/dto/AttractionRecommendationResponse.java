package com.lanyu.xiaolanaitravel.travel.dto;

import com.lanyu.xiaolanaitravel.explore.dto.AttractionResponse;

import java.util.List;

/**
 * 景点推荐结果。
 *
 * <p>各项分数是小兰内部的匹配分，不代表景点平台评分。</p>
 */
public record AttractionRecommendationResponse(
        AttractionResponse attraction,
        int matchPercentage,
        int profileScore,
        int tripScore,
        int favoriteScore,
        int geographyScore,
        int dislikePenalty,
        Integer nearestPlanDistanceMeters,
        boolean favorite,
        boolean profileUsed,
        List<String> recommendationReasons) {
}
