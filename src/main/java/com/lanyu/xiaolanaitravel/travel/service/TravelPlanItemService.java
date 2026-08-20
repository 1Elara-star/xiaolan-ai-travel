package com.lanyu.xiaolanaitravel.travel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lanyu.xiaolanaitravel.travel.entity.TravelPlanItem;
import com.lanyu.xiaolanaitravel.travel.dto.TravelPlanDraftItem;
import com.lanyu.xiaolanaitravel.travel.dto.TravelPlanItemRequest;
import com.lanyu.xiaolanaitravel.travel.dto.TravelPlanItemResponse;
import com.lanyu.xiaolanaitravel.travel.mapper.TravelPlanItemMapper;
import com.lanyu.xiaolanaitravel.explore.mapper.AttractionMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 旅行行程节点业务逻辑。
 */
@Service
public class TravelPlanItemService {

    private static final Set<String> ALLOWED_ITEM_TYPES = Set.of(
            "ATTRACTION",
            "FOOD",
            "HOTEL",
            "EVENT",
            "REST",
            "OTHER"
    );

    private final TravelPlanItemMapper travelPlanItemMapper;
    private final TravelPlanService travelPlanService;
    private final AttractionMapper attractionMapper;
    private final TravelDistanceService travelDistanceService;

    public TravelPlanItemService(
            TravelPlanItemMapper travelPlanItemMapper,
            TravelPlanService travelPlanService,
            AttractionMapper attractionMapper,
            TravelDistanceService travelDistanceService) {
        this.travelPlanItemMapper = travelPlanItemMapper;
        this.travelPlanService = travelPlanService;
        this.attractionMapper = attractionMapper;
        this.travelDistanceService = travelDistanceService;
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

    /**
     * 查询正式行程节点，并为同一天相邻、已定位的节点计算直线距离预览。
     *
     * <p>直线距离只用于帮助用户选择交通方式，不会写入数据库，
     * 也不会调用高德路线接口。实际道路距离仍需用户选择交通方式后再计算。</p>
     */
    public List<TravelPlanItemResponse> getMyPlanItemResponses(Long userId, Long planId) {
        List<TravelPlanItem> items = getMyPlanItems(userId, planId);
        List<TravelPlanItemResponse> responses = new ArrayList<>(items.size());
        TravelPlanItem previousItem = null;

        for (TravelPlanItem item : items) {
            Integer straightLineDistance = null;
            if (isImmediatelyAfterLocatedItemOnSameDay(previousItem, item)) {
                straightLineDistance = travelDistanceService.calculateStraightLineDistanceMeters(
                        previousItem.getLongitude(),
                        previousItem.getLatitude(),
                        item.getLongitude(),
                        item.getLatitude()
                );
            }
            responses.add(toResponse(item, straightLineDistance));
            previousItem = item;
        }

        return responses;
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

    /** 将用户明确选中的候选节点追加到正式行程，不删除已有节点。 */
    @Transactional
    public List<TravelPlanItemResponse> addFromDraft(
            Long userId,
            Long planId,
            List<TravelPlanDraftItem> draftItems) {

        var plan = travelPlanService.getMyPlanById(userId, planId);
        validateDraftItems(plan.getTravelDays(), draftItems);

        List<TravelPlanItem> existingItems = travelPlanItemMapper.selectList(
                new LambdaQueryWrapper<TravelPlanItem>()
                        .eq(TravelPlanItem::getPlanId, planId)
        );

        Map<Integer, Integer> nextOrderByDay = new HashMap<>();
        for (TravelPlanItem existingItem : existingItems) {
            nextOrderByDay.merge(
                    existingItem.getDayNumber(),
                    existingItem.getItemOrder(),
                    Math::max
            );
        }

        List<TravelPlanDraftItem> sortedDraftItems =
                new ArrayList<>(draftItems);
        sortedDraftItems.sort(
                Comparator.comparing(TravelPlanDraftItem::getDayNumber)
                        .thenComparing(TravelPlanDraftItem::getItemOrder)
        );

        try {
            for (TravelPlanDraftItem draftItem : sortedDraftItems) {
                TravelPlanItem item = new TravelPlanItem();
                item.setPlanId(planId);
                applyDraft(item, draftItem);
                int nextOrder = nextOrderByDay.merge(
                        draftItem.getDayNumber(),
                        1,
                        (current, ignored) -> current + 1
                );
                item.setItemOrder(nextOrder);
                travelPlanItemMapper.insert(item);
            }
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "候选节点加入正式行程时发生顺序冲突"
            );
        }

        return getMyPlanItemResponses(userId, planId);
    }

    public TravelPlanItemResponse toResponse(TravelPlanItem item) {
        return toResponse(item, null);
    }

    private TravelPlanItemResponse toResponse(
            TravelPlanItem item,
            Integer straightLineDistanceFromPrev) {
        String imageUrl = null;
        if (item.getAttractionId() != null) {
            var attraction = attractionMapper.selectById(item.getAttractionId());
            imageUrl = attraction == null ? null : attraction.getImageUrl();
        }
        return new TravelPlanItemResponse(item.getId(), item.getPlanId(), item.getDayNumber(),
                item.getItemOrder(), item.getItemType(), item.getAttractionId(), item.getPlaceName(),
                item.getAddress(), item.getLongitude(), item.getLatitude(), item.getCityCode(), item.getStartTime(),
                item.getEndTime(), item.getEndDayOffset(), item.getTransportMode(), item.getDistanceFromPrev(),
                item.getTravelTimeFromPrev(), straightLineDistanceFromPrev, item.getDescription(), imageUrl,
                item.getCreateTime(), item.getUpdateTime());
    }

    private boolean isImmediatelyAfterLocatedItemOnSameDay(
            TravelPlanItem previousItem,
            TravelPlanItem currentItem) {
        return previousItem != null
                && currentItem != null
                && previousItem.getDayNumber() != null
                && previousItem.getDayNumber().equals(currentItem.getDayNumber())
                && previousItem.getLongitude() != null
                && previousItem.getLatitude() != null
                && currentItem.getLongitude() != null
                && currentItem.getLatitude() != null;
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

    private void applyDraft(
            TravelPlanItem item,
            TravelPlanDraftItem draftItem) {

        item.setDayNumber(draftItem.getDayNumber());
        item.setItemOrder(draftItem.getItemOrder());
        item.setItemType(draftItem.getItemType());
        item.setAttractionId(draftItem.getAttractionId());
        item.setPlaceName(draftItem.getPlaceName().strip());
        item.setAddress(normalize(draftItem.getAddress()));
        item.setLongitude(draftItem.getLongitude());
        item.setLatitude(draftItem.getLatitude());
        item.setCityCode(normalize(draftItem.getCityCode()));
        item.setStartTime(draftItem.getStartTime());
        item.setEndTime(draftItem.getEndTime());
        item.setEndDayOffset(
                draftItem.getEndDayOffset() == null
                        ? 0
                        : draftItem.getEndDayOffset()
        );
        item.setTransportMode(normalize(draftItem.getTransportMode()));
        item.setDistanceFromPrev(draftItem.getDistanceFromPrev());
        item.setTravelTimeFromPrev(draftItem.getTravelTimeFromPrev());
        item.setDescription(normalize(draftItem.getDescription()));
    }

    private void validateDraftItems(
            Integer travelDays,
            List<TravelPlanDraftItem> draftItems) {

        if (draftItems == null || draftItems.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "候选行程没有可保存的节点"
            );
        }

        Set<String> positions = new HashSet<>();

        for (TravelPlanDraftItem item : draftItems) {
            if (item == null
                    || item.getDayNumber() == null
                    || item.getDayNumber() < 1
                    || item.getDayNumber() > travelDays
                    || item.getItemOrder() == null
                    || item.getItemOrder() < 1
                    || item.getPlaceName() == null
                    || item.getPlaceName().isBlank()
                    || item.getItemType() == null
                    || !ALLOWED_ITEM_TYPES.contains(item.getItemType())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "候选行程包含无效节点"
                );
            }

            validateTimeRange(
                    item.getStartTime(),
                    item.getEndTime(),
                    item.getEndDayOffset() == null
                            ? 0
                            : item.getEndDayOffset()
            );

            if (item.getAttractionId() != null
                    && attractionMapper.selectById(
                    item.getAttractionId()) == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "候选行程关联的收藏景点不存在"
                );
            }

            String position = item.getDayNumber()
                    + ":"
                    + item.getItemOrder();
            if (!positions.add(position)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "候选行程存在重复的节点顺序"
                );
            }
        }
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
