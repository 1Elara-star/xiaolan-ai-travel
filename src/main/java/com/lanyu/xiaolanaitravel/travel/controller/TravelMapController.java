package com.lanyu.xiaolanaitravel.travel.controller;

import com.lanyu.xiaolanaitravel.amap.dto.AmapTravelMode;
import com.lanyu.xiaolanaitravel.travel.dto.TravelItemLocationResponse;
import com.lanyu.xiaolanaitravel.travel.dto.TravelItemRouteResponse;
import com.lanyu.xiaolanaitravel.travel.service.TravelMapService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 当前用户旅行节点的高德地点匹配和路线计算接口。 */
@RestController
@RequestMapping("/travel/plan/{planId}/items/{itemId}/map")
public class TravelMapController {

    private final TravelMapService travelMapService;

    public TravelMapController(TravelMapService travelMapService) {
        this.travelMapService = travelMapService;
    }

    /** 一次高德 POI 调用：为单个节点补充真实地址和经纬度。 */
    @PostMapping("/location")
    public TravelItemLocationResponse resolveLocation(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable Long planId,
            @PathVariable Long itemId,
            @RequestParam(defaultValue = "false") boolean refresh) {
        return travelMapService.resolveItemLocation(userId, planId, itemId, refresh);
    }

    /** 一次高德路线调用：计算当前节点与前一个节点之间的距离和耗时。 */
    @PostMapping("/route-from-previous")
    public TravelItemRouteResponse calculateRouteFromPrevious(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable Long planId,
            @PathVariable Long itemId,
            @RequestParam(defaultValue = "WALKING") AmapTravelMode mode,
            @RequestParam(defaultValue = "false") boolean refresh) {
        return travelMapService.calculateRouteFromPrevious(
                userId, planId, itemId, mode, refresh);
    }
}
