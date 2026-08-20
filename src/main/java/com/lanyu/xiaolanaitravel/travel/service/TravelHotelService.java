package com.lanyu.xiaolanaitravel.travel.service;

import com.lanyu.xiaolanaitravel.ai.dto.FlyAiHotelResponse;
import com.lanyu.xiaolanaitravel.ai.service.FlyAiService;
import com.lanyu.xiaolanaitravel.travel.dto.HotelCandidateResponse;
import com.lanyu.xiaolanaitravel.travel.dto.HotelSearchCriteria;
import com.lanyu.xiaolanaitravel.user.service.UserProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * 旅行计划酒店候选业务逻辑。
 */
@Service
public class TravelHotelService {

    private final TravelPlanService travelPlanService;
    private final FlyAiService flyAiService;
    private final UserProfileService userProfileService;
    private final HotelRecommendationService hotelRecommendationService;

    public TravelHotelService(
            TravelPlanService travelPlanService,
            FlyAiService flyAiService,
            UserProfileService userProfileService,
            HotelRecommendationService hotelRecommendationService) {
        this.travelPlanService = travelPlanService;
        this.flyAiService = flyAiService;
        this.userProfileService = userProfileService;
        this.hotelRecommendationService = hotelRecommendationService;
    }

    /**
     * 根据当前用户的旅行计划查询飞猪真实酒店，
     * 并转换成小兰系统自己的酒店候选格式。
     */
    public List<HotelCandidateResponse> searchHotelsForPlan(
            Long userId,
            Long planId,
            HotelSearchCriteria criteria) {

        validateCriteria(criteria);

        // 1. 查询当前用户自己的旅行计划
        var plan = travelPlanService.getMyPlanById(userId, planId);

        // 2. 根据旅行目的地调用飞猪
        FlyAiHotelResponse response =
                flyAiService.searchHotels(
                        plan.getDestination(),
                        criteria.locationKeyword(),
                        criteria.maxPrice()
                );

        if (response == null || response.getData() == null
                || response.getData().getItemList() == null) {
            return List.of();
        }

        // 3. 使用本次行程条件和用户长期画像，对真实候选进行可解释排序。
        return hotelRecommendationService.rank(
                plan,
                userProfileService.getProfile(userId),
                response.getData().getItemList(),
                criteria);
    }

    private void validateCriteria(HotelSearchCriteria criteria) {
        if (criteria == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "酒店查询条件不能为空");
        }
        if (criteria.minPrice() != null && criteria.minPrice() < 0
                || criteria.maxPrice() != null && criteria.maxPrice() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "酒店价格不能小于0");
        }
        if (criteria.minPrice() != null && criteria.maxPrice() != null
                && criteria.minPrice() > criteria.maxPrice()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "最低价不能高于最高价");
        }
        if (criteria.locationKeyword() != null
                && criteria.locationKeyword().strip().length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "住宿位置名称不能超过100个字符");
        }
    }
}
