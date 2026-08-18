package com.lanyu.xiaolanaitravel.travel.controller;

import com.lanyu.xiaolanaitravel.ai.dto.AiTravelPlanResponse;
import com.lanyu.xiaolanaitravel.travel.service.TravelAiGenerationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 旅行行程生成接口。
 */
@RestController
@RequestMapping("/travel/plan/{planId}/ai")
public class TravelAiController {

    private final TravelAiGenerationService travelAiGenerationService;

    public TravelAiController(
            TravelAiGenerationService travelAiGenerationService) {
        this.travelAiGenerationService = travelAiGenerationService;
    }

    /**
     * 根据已经保存的旅行需求，
     * 调用 DeepSeek 生成完整行程并保存到 travel_plan_item。
     */
    @PostMapping("/generate")
    public ResponseEntity<AiTravelPlanResponse> generateTravelPlan(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable Long planId) {

        AiTravelPlanResponse result =
                travelAiGenerationService.generateAndSave(
                        userId,
                        planId
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(result);
    }
}