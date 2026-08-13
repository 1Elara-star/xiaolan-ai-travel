package com.lanyu.xiaolanaitravel.travel.controller;

import com.lanyu.xiaolanaitravel.travel.dto.TravelPlanItemRequest;
import com.lanyu.xiaolanaitravel.travel.dto.TravelPlanItemResponse;
import com.lanyu.xiaolanaitravel.travel.service.TravelPlanItemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
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
    public List<TravelPlanItemResponse> getMyPlanItems(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable Long planId) {
        return travelPlanItemService.getMyPlanItems(userId, planId).stream()
                .map(travelPlanItemService::toResponse).toList();
    }

    @PostMapping
    public ResponseEntity<TravelPlanItemResponse> create(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable Long planId,
            @Valid @RequestBody TravelPlanItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(travelPlanItemService.create(userId, planId, request));
    }

    @PutMapping("/{itemId}")
    public TravelPlanItemResponse update(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable Long planId,
            @PathVariable Long itemId,
            @Valid @RequestBody TravelPlanItemRequest request) {
        return travelPlanItemService.update(userId, planId, itemId, request);
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> delete(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable Long planId,
            @PathVariable Long itemId) {
        travelPlanItemService.delete(userId, planId, itemId);
        return ResponseEntity.noContent().build();
    }
}
