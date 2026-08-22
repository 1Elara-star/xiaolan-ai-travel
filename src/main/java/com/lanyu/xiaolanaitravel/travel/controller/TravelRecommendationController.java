package com.lanyu.xiaolanaitravel.travel.controller;

import com.lanyu.xiaolanaitravel.travel.dto.AttractionRecommendationResponse;
import com.lanyu.xiaolanaitravel.travel.service.AttractionRecommendationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 当前旅行的个性化景点推荐接口。 */
@RestController
@RequestMapping("/travel/plan/{planId}/recommendations")
public class TravelRecommendationController {

    private final AttractionRecommendationService recommendationService;

    public TravelRecommendationController(AttractionRecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/attractions")
    public List<AttractionRecommendationResponse> recommendAttractions(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable Long planId,
            @RequestParam(required = false) Integer limit) {
        return recommendationService.recommend(userId, planId, limit);
    }
}
