package com.lanyu.xiaolanaitravel.travel.controller;

import com.lanyu.xiaolanaitravel.ai.dto.FlyAiHotelResponse;
import com.lanyu.xiaolanaitravel.travel.service.TravelHotelService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 旅行计划酒店候选接口。
 */
@RestController
@RequestMapping("/travel/plan/{planId}/hotels")
public class TravelHotelController {

    private final TravelHotelService travelHotelService;

    public TravelHotelController(TravelHotelService travelHotelService) {
        this.travelHotelService = travelHotelService;
    }

    /**
     * 根据当前用户的旅行计划查询真实酒店候选。
     */
    @GetMapping
    public FlyAiHotelResponse getHotelsForPlan(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable Long planId) {

        return travelHotelService.searchHotelsForPlan(userId, planId);
    }
}