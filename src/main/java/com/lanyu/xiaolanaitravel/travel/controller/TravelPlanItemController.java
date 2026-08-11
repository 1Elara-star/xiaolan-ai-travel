package com.lanyu.xiaolanaitravel.travel.controller;

import com.lanyu.xiaolanaitravel.travel.entity.TravelPlanItem;
import com.lanyu.xiaolanaitravel.travel.service.TravelPlanItemService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 旅行行程节点接口。
 */
@RestController
@RequestMapping("/travel/plan/{planId}/items")
public class TravelPlanItemController {

    private final TravelPlanItemService travelPlanItemService;

    public TravelPlanItemController(TravelPlanItemService travelPlanItemService) {
        this.travelPlanItemService = travelPlanItemService;
    }

    /**
     * 按天和当天顺序查询当前用户的行程节点。
     */
    @GetMapping
    public List<TravelPlanItem> getMyPlanItems(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable Long planId) {
        return travelPlanItemService.getMyPlanItems(userId, planId);
    }
}
