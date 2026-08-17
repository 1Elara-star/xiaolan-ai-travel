package com.lanyu.xiaolanaitravel.travel.service;

import com.lanyu.xiaolanaitravel.ai.dto.FlyAiHotelResponse;
import com.lanyu.xiaolanaitravel.ai.service.FlyAiService;
import com.lanyu.xiaolanaitravel.travel.entity.TravelPlan;
import org.springframework.stereotype.Service;

@Service
public class TravelHotelService {

    private final TravelPlanService travelPlanService;
    private final FlyAiService flyAiService;

    public TravelHotelService(
            TravelPlanService travelPlanService,
            FlyAiService flyAiService) {
        this.travelPlanService = travelPlanService;
        this.flyAiService = flyAiService;
    }

    public FlyAiHotelResponse searchHotelsForPlan(
            Long userId,
            Long planId) {

        // 1. 查询当前用户自己的旅行计划
        TravelPlan plan =
                travelPlanService.getMyPlanById(userId, planId);

        // 2. 从旅行计划中获取目的地
        String destination = plan.getDestination();

        // 3. 调用飞猪查询真实酒店
        return flyAiService.searchHotels(
                destination,
                null,
                null
        );
    }
}