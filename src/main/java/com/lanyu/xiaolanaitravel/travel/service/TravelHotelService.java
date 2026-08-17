package com.lanyu.xiaolanaitravel.travel.service;

import com.lanyu.xiaolanaitravel.ai.dto.FlyAiHotelResponse;
import com.lanyu.xiaolanaitravel.ai.service.FlyAiService;
import com.lanyu.xiaolanaitravel.travel.dto.HotelCandidateResponse;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 旅行计划酒店候选业务逻辑。
 */
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

    /**
     * 根据当前用户的旅行计划查询飞猪真实酒店，
     * 并转换成小兰系统自己的酒店候选格式。
     */
    public List<HotelCandidateResponse> searchHotelsForPlan(
            Long userId,
            Long planId) {

        // 1. 查询当前用户自己的旅行计划
        var plan = travelPlanService.getMyPlanById(userId, planId);

        // 2. 根据旅行目的地调用飞猪
        FlyAiHotelResponse response =
                flyAiService.searchHotels(
                        plan.getDestination(),
                        null,
                        null
                );

        // 3. 飞猪数据格式 → 小兰自己的酒店格式
        return response.getData()
                .getItemList()
                .stream()
                .map(item -> new HotelCandidateResponse(
                        item.getName(),
                        item.getPrice(),
                        item.getAddress(),
                        item.getLatitude(),
                        item.getLongitude(),
                        item.getMainPic(),
                        item.getDetailUrl(),
                        item.getStar(),
                        item.getBrandName(),
                        "FLIGGY"
                ))
                .toList();
    }
}