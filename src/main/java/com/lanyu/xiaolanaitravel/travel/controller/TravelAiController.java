package com.lanyu.xiaolanaitravel.travel.controller;

import com.lanyu.xiaolanaitravel.travel.dto.TravelDraftSessionResponse;
import com.lanyu.xiaolanaitravel.travel.dto.TravelAiGenerateRequest;
import com.lanyu.xiaolanaitravel.travel.service.TravelAiGenerationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
     * 调用 DeepSeek 生成候选行程并创建临时 Draft Session。
     * 地图信息仍由用户后续按需补全。
     */
    @PostMapping("/generate")
    public ResponseEntity<TravelDraftSessionResponse> generateTravelPlan(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable Long planId,
            @Valid @RequestBody(required = false) TravelAiGenerateRequest request) {

        TravelDraftSessionResponse result =
                travelAiGenerationService.generateDraftSession(
                        userId,
                        planId,
                        request == null
                                ? null
                                : request.additionalRequirements()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(result);
    }
}
