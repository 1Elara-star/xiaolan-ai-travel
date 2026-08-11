package com.lanyu.xiaolanaitravel.travel.service;

import com.lanyu.xiaolanaitravel.travel.dto.TravelPlanRequest;
import com.lanyu.xiaolanaitravel.travel.entity.TravelPlan;
import com.lanyu.xiaolanaitravel.travel.mapper.TravelPlanMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.temporal.ChronoUnit;

/**
 * 旅行计划业务逻辑
 */
@Service
public class TravelPlanService {

    private final TravelPlanMapper travelPlanMapper;

    public TravelPlanService(TravelPlanMapper travelPlanMapper) {
        this.travelPlanMapper = travelPlanMapper;
    }

    /**
     * 创建旅行计划
     *
     * userId 不由前端传入，
     * 而是由 JWT 中获取当前登录用户ID。
     */
    public TravelPlan createPlan(
            Long userId,
            TravelPlanRequest request) {

        // 1. 检查旅行日期是否合法
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "结束日期不能早于出发日期"
            );
        }

        // 2. 自动计算旅行天数
        // 例如 8月10日～8月12日 = 3天
        int travelDays = (int) ChronoUnit.DAYS.between(
                request.getStartDate(),
                request.getEndDate()
        ) + 1;

        // 3. 创建旅行计划对象
        TravelPlan plan = new TravelPlan();

        // 当前登录用户，由JWT确定
        plan.setUserId(userId);

        plan.setTitle(request.getTitle());
        plan.setDepartureCity(request.getDepartureCity());
        plan.setDestination(request.getDestination());
        plan.setStartDate(request.getStartDate());
        plan.setEndDate(request.getEndDate());

        // 后端自动计算
        plan.setTravelDays(travelDays);

        plan.setPeopleCount(request.getPeopleCount());
        plan.setCompanionType(request.getCompanionType());
        plan.setBudget(request.getBudget());
        plan.setTripType(request.getTripType());
        plan.setTripPreferences(request.getTripPreferences());
        plan.setSpecialRequirements(request.getSpecialRequirements());

        // 刚创建时还没有AI行程
        plan.setPlanContent(null);

        // 初始状态：规划中
        plan.setTripStatus("PLANNING");

        // 4. 写入数据库
        travelPlanMapper.insert(plan);

        // 5. 返回创建后的旅行计划
        return plan;
    }
}