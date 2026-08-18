package com.lanyu.xiaolanaitravel.travel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lanyu.xiaolanaitravel.travel.entity.TravelPlanItem;
import com.lanyu.xiaolanaitravel.travel.dto.TravelPlanItemRequest;
import com.lanyu.xiaolanaitravel.travel.dto.TravelPlanItemResponse;
import com.lanyu.xiaolanaitravel.travel.mapper.TravelPlanItemMapper;
import com.lanyu.xiaolanaitravel.explore.mapper.AttractionMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * 旅行行程节点业务逻辑。
 */
@Service
public class TravelPlanItemService {

    private final TravelPlanItemMapper travelPlanItemMapper;
    private final TravelPlanService travelPlanService;
    private final AttractionMapper attractionMapper;

    public TravelPlanItemService(
            TravelPlanItemMapper travelPlanItemMapper,
            TravelPlanService travelPlanService,
            AttractionMapper attractionMapper) {
        this.travelPlanItemMapper = travelPlanItemMapper;
        this.travelPlanService = travelPlanService;
        this.attractionMapper = attractionMapper;
    }

    /**
     * 查询当前登录用户某个旅行计划下的全部行程节点。
     *
     * <p>先校验旅行计划归属，再查询节点，避免通过 planId
     * 读取其他用户的私人行程。</p>
     */
    public List<TravelPlanItem> getMyPlanItems(Long userId, Long planId) {
        travelPlanService.getMyPlanById(userId, planId);

        LambdaQueryWrapper<TravelPlanItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TravelPlanItem::getPlanId, planId)
                .orderByAsc(TravelPlanItem::getDayNumber)
                .orderByAsc(TravelPlanItem::getItemOrder)
                .orderByAsc(TravelPlanItem::getId);

        return travelPlanItemMapper.selectList(wrapper);
    }

    public TravelPlanItemResponse create(
            Long userId, Long planId, TravelPlanItemRequest request) {
        validateRequest(userId, planId, request);
        TravelPlanItem item = new TravelPlanItem();
        item.setPlanId(planId);
        apply(item, request);
        ensurePositionAvailable(planId, request.dayNumber(), request.itemOrder(), null);
        try {
            travelPlanItemMapper.insert(item);
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "当天已有相同顺序的行程节点");
        }
        return toResponse(travelPlanItemMapper.selectById(item.getId()));
    }

    public TravelPlanItemResponse update(
            Long userId, Long planId, Long itemId, TravelPlanItemRequest request) {
        validateRequest(userId, planId, request);
        TravelPlanItem item = getOwnedItem(planId, itemId);
        ensurePositionAvailable(planId, request.dayNumber(), request.itemOrder(), itemId);
        apply(item, request);
        try {
            if (travelPlanItemMapper.updateById(item) == 0) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "行程节点不存在");
            }
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "当天已有相同顺序的行程节点");
        }
        return toResponse(getOwnedItem(planId, itemId));
    }

    public void delete(Long userId, Long planId, Long itemId) {
        travelPlanService.getMyPlanById(userId, planId);
        TravelPlanItem item = getOwnedItem(planId, itemId);
        if (travelPlanItemMapper.deleteById(item.getId()) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "行程节点不存在");
        }
    }

    public TravelPlanItemResponse toResponse(TravelPlanItem item) {
        return new TravelPlanItemResponse(item.getId(), item.getPlanId(), item.getDayNumber(),
                item.getItemOrder(), item.getItemType(), item.getAttractionId(), item.getPlaceName(),
                item.getAddress(), item.getLongitude(), item.getLatitude(), item.getCityCode(), item.getStartTime(),
                item.getEndTime(), item.getEndDayOffset(), item.getTransportMode(), item.getDistanceFromPrev(),
                item.getTravelTimeFromPrev(), item.getDescription(), item.getCreateTime(),
                item.getUpdateTime());
    }

    private void validateRequest(Long userId, Long planId, TravelPlanItemRequest request) {
        var plan = travelPlanService.getMyPlanById(userId, planId);
        if (request.dayNumber() > plan.getTravelDays()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "节点天数超出行程总天数");
        }
        validateTimeRange(request.startTime(), request.endTime(), resolveEndDayOffset(request));
        if (request.attractionId() != null
                && attractionMapper.selectById(request.attractionId()) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "关联景点不存在");
        }
    }

    private TravelPlanItem getOwnedItem(Long planId, Long itemId) {
        TravelPlanItem item = travelPlanItemMapper.selectOne(
                new LambdaQueryWrapper<TravelPlanItem>()
                        .eq(TravelPlanItem::getId, itemId)
                        .eq(TravelPlanItem::getPlanId, planId));
        if (item == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "行程节点不存在");
        }
        return item;
    }

    private void apply(TravelPlanItem item, TravelPlanItemRequest request) {
        item.setDayNumber(request.dayNumber());
        item.setItemOrder(request.itemOrder());
        item.setItemType(request.itemType());
        item.setAttractionId(request.attractionId());
        item.setPlaceName(request.placeName().strip());
        item.setAddress(normalize(request.address()));
        item.setLongitude(request.longitude());
        item.setLatitude(request.latitude());
        item.setStartTime(request.startTime());
        item.setEndTime(request.endTime());
        item.setEndDayOffset(resolveEndDayOffset(request));
        item.setTransportMode(normalize(request.transportMode()));
        item.setDistanceFromPrev(request.distanceFromPrev());
        item.setTravelTimeFromPrev(request.travelTimeFromPrev());
        item.setDescription(normalize(request.description()));
    }

    private void ensurePositionAvailable(Long planId, Integer day, Integer order, Long excludedId) {
        LambdaQueryWrapper<TravelPlanItem> query = new LambdaQueryWrapper<TravelPlanItem>()
                .eq(TravelPlanItem::getPlanId, planId)
                .eq(TravelPlanItem::getDayNumber, day)
                .eq(TravelPlanItem::getItemOrder, order)
                .ne(excludedId != null, TravelPlanItem::getId, excludedId);
        if (travelPlanItemMapper.selectCount(query) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "当天已有相同顺序的行程节点");
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private int resolveEndDayOffset(TravelPlanItemRequest request) {
        return request.endDayOffset() == null ? 0 : request.endDayOffset();
    }

    private void validateTimeRange(
            java.time.LocalTime startTime,
            java.time.LocalTime endTime,
            int endDayOffset) {
        if (endDayOffset == 1 && (startTime == null || endTime == null)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "跨天节点必须同时填写开始时间和结束时间");
        }
        if (startTime != null && endTime != null
                && endDayOffset == 0 && !endTime.isAfter(startTime)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "当天结束的节点，结束时间必须晚于开始时间；跨天请将endDayOffset设为1");
        }
    }
}
