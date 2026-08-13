package com.lanyu.xiaolanaitravel.travel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lanyu.xiaolanaitravel.travel.dto.TravelPlanRequest;
import com.lanyu.xiaolanaitravel.travel.dto.TravelPlanResponse;
import com.lanyu.xiaolanaitravel.travel.entity.TravelPlan;
import com.lanyu.xiaolanaitravel.travel.mapper.TravelPlanMapper;
import com.lanyu.xiaolanaitravel.travel.entity.TravelPlanItem;
import com.lanyu.xiaolanaitravel.travel.mapper.TravelPlanItemMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 旅行计划业务逻辑
 */
@Service
public class TravelPlanService {

    private final TravelPlanMapper travelPlanMapper;
    private final TravelPlanItemMapper travelPlanItemMapper;

    public TravelPlanService(
            TravelPlanMapper travelPlanMapper,
            TravelPlanItemMapper travelPlanItemMapper) {
        this.travelPlanMapper = travelPlanMapper;
        this.travelPlanItemMapper = travelPlanItemMapper;
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

        plan.setTitle(request.getTitle().strip());
        plan.setDepartureCity(request.getDepartureCity().strip());
        plan.setDestination(request.getDestination().strip());
        plan.setStartDate(request.getStartDate());
        plan.setEndDate(request.getEndDate());

        // 后端自动计算
        plan.setTravelDays(travelDays);

        plan.setPeopleCount(request.getPeopleCount());
        plan.setCompanionType(normalize(request.getCompanionType()));
        plan.setBudget(request.getBudget());
        plan.setTripType(normalize(request.getTripType()));
        plan.setTripPreferences(normalize(request.getTripPreferences()));
        plan.setSpecialRequirements(normalize(request.getSpecialRequirements()));

        // 刚创建时还没有AI行程
        plan.setPlanContent(null);

        // 初始状态：规划中
        plan.setTripStatus("PLANNING");

        // 4. 写入数据库
        if (travelPlanMapper.insert(plan) == 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "创建旅行计划失败");
        }

        // 5. 返回创建后的旅行计划
        return getMyPlanById(userId, plan.getId());
    }

    /**
     * 查询当前登录用户自己的旅行计划。
     *
     * userId 由 JWT 提供，查询条件由后端控制，
     * 避免用户查看其他人的私人旅行数据。
     */
    public List<TravelPlan> getMyPlans(Long userId) {
        LambdaQueryWrapper<TravelPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TravelPlan::getUserId, userId)
                .orderByDesc(TravelPlan::getCreateTime);
        return travelPlanMapper.selectList(wrapper);
    }

    /**
     * 查询当前登录用户自己的单个旅行计划。
     */
    public TravelPlan getMyPlanById(Long userId, Long planId) {
        LambdaQueryWrapper<TravelPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TravelPlan::getId, planId)
                .eq(TravelPlan::getUserId, userId);

        TravelPlan plan = travelPlanMapper.selectOne(wrapper);
        if (plan == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "旅行计划不存在");
        }
        return plan;
    }

    /**
     * 修改当前登录用户自己的旅行需求。
     *
     * 关键需求发生变化后，旧的 AI 方案不再可靠，
     * 因此清空方案内容并重新进入规划状态。
     */
    @Transactional
    public TravelPlan updateMyPlan(
            Long userId,
            Long planId,
            TravelPlanRequest request) {

        TravelPlan plan = getMyPlanById(userId, planId);
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "结束日期不能早于出发日期"
            );
        }

        int travelDays = (int) ChronoUnit.DAYS.between(
                request.getStartDate(),
                request.getEndDate()
        ) + 1;

        Long outOfRangeNodes = travelPlanItemMapper.selectCount(
                new LambdaQueryWrapper<TravelPlanItem>()
                        .eq(TravelPlanItem::getPlanId, planId)
                        .gt(TravelPlanItem::getDayNumber, travelDays));
        if (outOfRangeNodes > 0) {
            travelPlanItemMapper.delete(new LambdaQueryWrapper<TravelPlanItem>()
                    .eq(TravelPlanItem::getPlanId, planId)
                    .gt(TravelPlanItem::getDayNumber, travelDays));
        }

        plan.setTitle(request.getTitle().strip());
        plan.setDepartureCity(request.getDepartureCity().strip());
        plan.setDestination(request.getDestination().strip());
        plan.setStartDate(request.getStartDate());
        plan.setEndDate(request.getEndDate());
        plan.setTravelDays(travelDays);
        plan.setPeopleCount(request.getPeopleCount());
        plan.setCompanionType(normalize(request.getCompanionType()));
        plan.setBudget(request.getBudget());
        plan.setTripType(normalize(request.getTripType()));
        plan.setTripPreferences(normalize(request.getTripPreferences()));
        plan.setSpecialRequirements(normalize(request.getSpecialRequirements()));
        plan.setPlanContent(null);
        plan.setTripStatus("PLANNING");
        if (travelPlanMapper.updateById(plan) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "旅行计划不存在");
        }
        return getMyPlanById(userId, planId);
    }

    /**
     * 删除当前登录用户自己的旅行计划。
     *
     * 当前阶段采用物理删除；开始使用行程节点、反馈等关联数据前，
     * 需要升级为事务内关联删除或重新设计逻辑删除。
     */
    @Transactional
    public void deleteMyPlan(Long userId, Long planId) {
        TravelPlan plan = getMyPlanById(userId, planId);
        travelPlanItemMapper.delete(new LambdaQueryWrapper<TravelPlanItem>()
                .eq(TravelPlanItem::getPlanId, plan.getId()));
        if (travelPlanMapper.deleteById(plan.getId()) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "旅行计划不存在");
        }
    }

    public TravelPlanResponse toResponse(TravelPlan plan) {
        return new TravelPlanResponse(plan.getId(), plan.getTitle(), plan.getDepartureCity(),
                plan.getDestination(), plan.getStartDate(), plan.getEndDate(), plan.getTravelDays(),
                plan.getPeopleCount(), plan.getCompanionType(), plan.getBudget(), plan.getTripType(),
                plan.getTripPreferences(), plan.getSpecialRequirements(), plan.getTripStatus(),
                plan.getCreateTime(), plan.getUpdateTime());
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
