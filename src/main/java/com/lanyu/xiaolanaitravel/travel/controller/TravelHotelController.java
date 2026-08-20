package com.lanyu.xiaolanaitravel.travel.controller;

import com.lanyu.xiaolanaitravel.travel.dto.HotelCandidateResponse;
import com.lanyu.xiaolanaitravel.travel.dto.HotelLocationType;
import com.lanyu.xiaolanaitravel.travel.dto.HotelPreferenceParseRequest;
import com.lanyu.xiaolanaitravel.travel.dto.HotelSearchCriteria;
import com.lanyu.xiaolanaitravel.travel.service.HotelPreferenceParsingService;
import com.lanyu.xiaolanaitravel.travel.service.TravelHotelService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 旅行计划酒店候选接口。
 */
@RestController
@RequestMapping("/travel/plan/{planId}/hotels")
public class TravelHotelController {

    private final TravelHotelService travelHotelService;
    private final HotelPreferenceParsingService hotelPreferenceParsingService;

    public TravelHotelController(
            TravelHotelService travelHotelService,
            HotelPreferenceParsingService hotelPreferenceParsingService) {
        this.travelHotelService = travelHotelService;
        this.hotelPreferenceParsingService = hotelPreferenceParsingService;
    }

    /**
     * 根据当前用户的旅行计划查询真实酒店候选。
     */
    @GetMapping
    public List<HotelCandidateResponse> getHotelsForPlan(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable Long planId,
            @RequestParam(required = false) HotelLocationType locationType,
            @RequestParam(required = false) String locationKeyword,
            // 兼容旧前端，后续统一使用 locationKeyword。
            @RequestParam(required = false) String businessArea,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice) {

        String keyword = locationKeyword == null || locationKeyword.isBlank()
                ? businessArea
                : locationKeyword;
        HotelLocationType resolvedType = locationType == null
                && keyword != null && !keyword.isBlank()
                ? HotelLocationType.BUSINESS_AREA
                : locationType;

        return travelHotelService.searchHotelsForPlan(
                userId, planId,
                new HotelSearchCriteria(resolvedType, keyword, minPrice, maxPrice));
    }

    /** 将用户自然语言住宿需求整理为可编辑的结构化条件。 */
    @PostMapping("/preferences/parse")
    public HotelSearchCriteria parsePreference(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable Long planId,
            @Valid @RequestBody HotelPreferenceParseRequest request) {
        return hotelPreferenceParsingService.parse(
                userId,
                planId,
                request.preference());
    }
}
