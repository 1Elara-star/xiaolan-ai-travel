package com.lanyu.xiaolanaitravel.travel.controller;

import com.lanyu.xiaolanaitravel.travel.dto.TravelPlanRequest;
import com.lanyu.xiaolanaitravel.travel.entity.TravelPlan;
import com.lanyu.xiaolanaitravel.travel.service.TravelPlanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    /**
     * 查询当前登录用户自己的旅行计划。
     */
    @GetMapping("/my")
    public List<TravelPlan> getMyPlans(
            @RequestAttribute("currentUserId") Long userId) {
        return travelPlanService.getMyPlans(userId);
    }

    /**
     * 查看当前登录用户自己的单个旅行计划。
     */
    @GetMapping("/{id}")
    public TravelPlan getMyPlanById(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable Long id) {
        return travelPlanService.getMyPlanById(userId, id);
    }

    /**
     * 修改当前登录用户自己的旅行需求。
     */
    @PutMapping("/{id}")
    public TravelPlan updateMyPlan(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable Long id,
            @Valid @RequestBody TravelPlanRequest request) {
        return travelPlanService.updateMyPlan(userId, id, request);
    }

    /**
     * 删除当前登录用户自己的旅行计划。
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMyPlan(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable Long id) {
        travelPlanService.deleteMyPlan(userId, id);
        return ResponseEntity.noContent().build();
    }
}
