package com.lanyu.xiaolanaitravel.travel.controller;

import com.lanyu.xiaolanaitravel.travel.dto.TravelPlanRequest;
import com.lanyu.xiaolanaitravel.travel.entity.TravelPlan;
import com.lanyu.xiaolanaitravel.travel.service.TravelPlanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 旅行计划接口
 */
@RestController
@RequestMapping("/travel/plan")
public class TravelPlanController {

    private final TravelPlanService travelPlanService;

    public TravelPlanController(TravelPlanService travelPlanService) {
        this.travelPlanService = travelPlanService;
    }

    /**
     * 创建旅行计划
     */
    @PostMapping
    public ResponseEntity<TravelPlan> createPlan(
            @RequestAttribute("currentUserId") Long userId,
            @Valid @RequestBody TravelPlanRequest request) {

        TravelPlan plan =
                travelPlanService.createPlan(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(plan);
    }
}