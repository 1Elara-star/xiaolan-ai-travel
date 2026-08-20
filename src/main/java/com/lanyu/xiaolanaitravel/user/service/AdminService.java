package com.lanyu.xiaolanaitravel.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lanyu.xiaolanaitravel.explore.entity.Attraction;
import com.lanyu.xiaolanaitravel.explore.mapper.AttractionMapper;
import com.lanyu.xiaolanaitravel.favorite.mapper.AttractionFavoriteMapper;
import com.lanyu.xiaolanaitravel.memory.mapper.UserMemoryMapper;
import com.lanyu.xiaolanaitravel.travel.entity.TravelPlan;
import com.lanyu.xiaolanaitravel.travel.mapper.TravelPlanMapper;
import com.lanyu.xiaolanaitravel.user.dto.AdminAttractionResponse;
import com.lanyu.xiaolanaitravel.user.dto.AdminOverviewResponse;
import com.lanyu.xiaolanaitravel.user.dto.AdminPlanResponse;
import com.lanyu.xiaolanaitravel.user.dto.AdminUserResponse;
import com.lanyu.xiaolanaitravel.user.entity.User;
import com.lanyu.xiaolanaitravel.user.mapper.UserMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminService {
    private static final int LIST_LIMIT = 100;

    private final UserMapper userMapper;
    private final TravelPlanMapper travelPlanMapper;
    private final AttractionMapper attractionMapper;
    private final AttractionFavoriteMapper favoriteMapper;
    private final UserMemoryMapper memoryMapper;

    public AdminService(UserMapper userMapper, TravelPlanMapper travelPlanMapper,
                        AttractionMapper attractionMapper, AttractionFavoriteMapper favoriteMapper,
                        UserMemoryMapper memoryMapper) {
        this.userMapper = userMapper;
        this.travelPlanMapper = travelPlanMapper;
        this.attractionMapper = attractionMapper;
        this.favoriteMapper = favoriteMapper;
        this.memoryMapper = memoryMapper;
    }

    public AdminOverviewResponse overview() {
        long admins = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getRole, "ADMIN"));
        return new AdminOverviewResponse(
                userMapper.selectCount(null), admins, travelPlanMapper.selectCount(null),
                attractionMapper.selectCount(null), favoriteMapper.selectCount(null),
                memoryMapper.selectCount(null));
    }

    public List<AdminUserResponse> users(String keyword) {
        String query = normalize(keyword);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .and(query != null, nested -> nested.like(User::getUsername, query)
                        .or().like(User::getNickname, query)
                        .or().like(User::getEmail, query))
                .orderByDesc(User::getCreateTime)
                .last("LIMIT " + LIST_LIMIT);
        return userMapper.selectList(wrapper).stream().map(this::toUserResponse).toList();
    }

    @Transactional
    public AdminUserResponse updateRole(Long currentAdminId, Long userId, String role) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在");
        if (currentAdminId.equals(userId) && !"ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "不能取消自己的管理员身份");
        }
        if ("ADMIN".equals(user.getRole()) && !"ADMIN".equals(role)) {
            long adminCount = userMapper.selectCount(
                    new LambdaQueryWrapper<User>().eq(User::getRole, "ADMIN"));
            if (adminCount <= 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "系统必须保留至少一个管理员");
            }
        }
        user.setRole(role);
        if (userMapper.updateById(user) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在");
        }
        return toUserResponse(userMapper.selectById(userId));
    }

    public List<AdminPlanResponse> plans(String keyword) {
        String query = normalize(keyword);
        LambdaQueryWrapper<TravelPlan> wrapper = new LambdaQueryWrapper<TravelPlan>()
                .and(query != null, nested -> nested.like(TravelPlan::getTitle, query)
                        .or().like(TravelPlan::getDestination, query)
                        .or().like(TravelPlan::getDepartureCity, query))
                .orderByDesc(TravelPlan::getCreateTime)
                .last("LIMIT " + LIST_LIMIT);
        List<TravelPlan> plans = travelPlanMapper.selectList(wrapper);
        List<Long> userIds = plans.stream().map(TravelPlan::getUserId).distinct().toList();
        Map<Long, User> users = userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return plans.stream().map(plan -> {
            User owner = users.get(plan.getUserId());
            return new AdminPlanResponse(plan.getId(), plan.getTitle(), plan.getUserId(),
                    owner == null ? "未知用户" : owner.getUsername(), plan.getDestination(),
                    plan.getStartDate(), plan.getEndDate(), plan.getTravelDays(),
                    plan.getTripStatus(), plan.getCreateTime());
        }).toList();
    }

    public List<AdminAttractionResponse> attractions(String keyword) {
        String query = normalize(keyword);
        LambdaQueryWrapper<Attraction> wrapper = new LambdaQueryWrapper<Attraction>()
                .and(query != null, nested -> nested.like(Attraction::getName, query)
                        .or().like(Attraction::getCity, query)
                        .or().like(Attraction::getType, query))
                .orderByDesc(Attraction::getCreateTime)
                .last("LIMIT " + LIST_LIMIT);
        return attractionMapper.selectList(wrapper).stream()
                .map(item -> new AdminAttractionResponse(item.getId(), item.getName(), item.getCity(),
                        item.getType(), item.getAddress(), item.getCreateTime()))
                .toList();
    }

    private AdminUserResponse toUserResponse(User user) {
        return new AdminUserResponse(user.getId(), user.getUsername(), user.getNickname(),
                user.getEmail(), user.getPhone(), user.getRole(), user.getCreateTime(),
                user.getUpdateTime());
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
