package com.lanyu.xiaolanaitravel.travel.service;

import com.lanyu.xiaolanaitravel.travel.dto.TravelPlanDraft;
import com.lanyu.xiaolanaitravel.travel.dto.TravelPlanDraftItem;
import com.lanyu.xiaolanaitravel.travel.dto.TravelValidationIssue;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 对候选行程执行确定性的本地校验。
 *
 * 该服务只读取 TravelPlanDraft，不调用大模型、地图 API 或数据库，
 * 也不会自动修改候选行程。校验结果后续可以展示给用户，
 * 或作为 Repair Agent 的明确问题输入。
 */
@Service
public class TravelPlanDraftValidationService {

    private static final int MINUTES_PER_DAY = 24 * 60;

    private static final Set<String> ALLOWED_ITEM_TYPES = Set.of(
            "ATTRACTION",
            "FOOD",
            "HOTEL",
            "EVENT",
            "REST",
            "OTHER"
    );

    /**
     * 返回候选行程中发现的全部问题；没有问题时返回空列表。
     */
    public List<TravelValidationIssue> validate(TravelPlanDraft draft) {
        if (draft == null) {
            throw new IllegalArgumentException("候选行程不能为空");
        }

        List<TravelValidationIssue> issues = new ArrayList<>();
        Integer travelDays = draft.getTravelDays();
        boolean validTravelDays = travelDays != null && travelDays > 0;

        if (!validTravelDays) {
            issues.add(issue(
                    "INVALID_TRAVEL_DAYS",
                    "ERROR",
                    null,
                    null,
                    "候选行程缺少有效的旅行天数"
            ));
        }

        List<TravelPlanDraftItem> items = draft.getItems();
        if (items == null || items.isEmpty()) {
            issues.add(issue(
                    "EMPTY_DRAFT",
                    "ERROR",
                    null,
                    null,
                    "候选行程没有任何节点"
            ));
            return List.copyOf(issues);
        }

        Set<String> occupiedPositions = new HashSet<>();
        Set<Integer> daysWithItems = new HashSet<>();
        List<TimedDraftItem> timedItems = new ArrayList<>();

        for (TravelPlanDraftItem item : items) {
            if (item == null) {
                issues.add(issue(
                        "NULL_ITEM",
                        "ERROR",
                        null,
                        null,
                        "候选行程包含空节点"
                ));
                continue;
            }

            String itemKey = itemKey(item);
            boolean validDayNumber = item.getDayNumber() != null
                    && item.getDayNumber() > 0
                    && (!validTravelDays || item.getDayNumber() <= travelDays);

            if (!validDayNumber) {
                issues.add(issue(
                        "INVALID_DAY_NUMBER",
                        "ERROR",
                        itemKey,
                        null,
                        "节点的 dayNumber 超出旅行天数范围"
                ));
            } else {
                daysWithItems.add(item.getDayNumber());
            }

            boolean validItemOrder = item.getItemOrder() != null
                    && item.getItemOrder() > 0;
            if (!validItemOrder) {
                issues.add(issue(
                        "INVALID_ITEM_ORDER",
                        "ERROR",
                        itemKey,
                        null,
                        "节点缺少有效的当天顺序"
                ));
            }

            if (validDayNumber && validItemOrder) {
                String position = item.getDayNumber() + ":" + item.getItemOrder();
                if (!occupiedPositions.add(position)) {
                    issues.add(issue(
                            "DUPLICATE_ITEM_ORDER",
                            "ERROR",
                            itemKey,
                            null,
                            "同一天存在重复的节点顺序"
                    ));
                }
            }

            if (isBlank(item.getPlaceName())) {
                issues.add(issue(
                        "MISSING_PLACE_NAME",
                        "ERROR",
                        itemKey,
                        null,
                        "节点缺少地点名称"
                ));
            }

            if (isBlank(item.getItemType())
                    || !ALLOWED_ITEM_TYPES.contains(item.getItemType())) {
                issues.add(issue(
                        "INVALID_ITEM_TYPE",
                        "ERROR",
                        itemKey,
                        null,
                        "节点类型不在系统支持范围内"
                ));
            }

            validateLocation(item, itemKey, issues);
            validateRouteNumbers(item, itemKey, issues);
            validateTime(item, itemKey, validDayNumber, validTravelDays,
                    travelDays, timedItems, issues);
        }

        if (validTravelDays) {
            for (int dayNumber = 1; dayNumber <= travelDays; dayNumber++) {
                if (!daysWithItems.contains(dayNumber)) {
                    issues.add(issue(
                            "EMPTY_DAY",
                            "WARNING",
                            null,
                            null,
                            "第 " + dayNumber + " 天没有安排任何节点"
                    ));
                }
            }
        }

        validateTimeline(timedItems, issues);
        return List.copyOf(issues);
    }

    /**
     * 判断校验结果中是否包含必须处理的错误。
     */
    public boolean hasErrors(List<TravelValidationIssue> issues) {
        return issues != null && issues.stream()
                .anyMatch(issue -> "ERROR".equals(issue.severity()));
    }

    private void validateLocation(
            TravelPlanDraftItem item,
            String itemKey,
            List<TravelValidationIssue> issues) {

        boolean hasLongitude = item.getLongitude() != null;
        boolean hasLatitude = item.getLatitude() != null;

        if (hasLongitude != hasLatitude) {
            issues.add(issue(
                    "INCOMPLETE_COORDINATES",
                    "ERROR",
                    itemKey,
                    null,
                    "地点经纬度不完整"
            ));
            return;
        }

        boolean hasPoiMatch = !isBlank(item.getPoiId())
                || !isBlank(item.getMatchedPoiName());
        if (hasPoiMatch && !hasLongitude) {
            issues.add(issue(
                    "POI_COORDINATES_MISSING",
                    "WARNING",
                    itemKey,
                    null,
                    "地点已有 POI 匹配信息，但缺少经纬度"
            ));
        }
    }

    private void validateRouteNumbers(
            TravelPlanDraftItem item,
            String itemKey,
            List<TravelValidationIssue> issues) {

        if (isNegative(item.getDistanceFromPrev())
                || isNegative(item.getTravelTimeFromPrev())
                || isNegative(item.getStraightLineDistanceFromPrev())) {
            issues.add(issue(
                    "INVALID_ROUTE_DATA",
                    "ERROR",
                    itemKey,
                    null,
                    "节点包含负数距离或交通耗时"
            ));
        }
    }

    private void validateTime(
            TravelPlanDraftItem item,
            String itemKey,
            boolean validDayNumber,
            boolean validTravelDays,
            Integer travelDays,
            List<TimedDraftItem> timedItems,
            List<TravelValidationIssue> issues) {

        LocalTime startTime = item.getStartTime();
        LocalTime endTime = item.getEndTime();
        Integer endDayOffset = item.getEndDayOffset();

        if ((startTime == null) != (endTime == null)) {
            issues.add(issue(
                    "INCOMPLETE_TIME_RANGE",
                    "ERROR",
                    itemKey,
                    null,
                    "节点必须同时填写开始时间和结束时间"
            ));
            return;
        }

        if (endDayOffset == null || (endDayOffset != 0 && endDayOffset != 1)) {
            issues.add(issue(
                    "INVALID_END_DAY_OFFSET",
                    "ERROR",
                    itemKey,
                    null,
                    "endDayOffset 只能是 0 或 1"
            ));
            return;
        }

        if (startTime == null) {
            if (endDayOffset == 1) {
                issues.add(issue(
                        "CROSS_DAY_TIME_MISSING",
                        "ERROR",
                        itemKey,
                        null,
                        "跨天节点必须填写完整的开始和结束时间"
                ));
            }
            return;
        }

        if (endDayOffset == 0 && !endTime.isAfter(startTime)) {
            issues.add(issue(
                    "INVALID_TIME_RANGE",
                    "ERROR",
                    itemKey,
                    null,
                    "当天结束的节点，结束时间必须晚于开始时间"
            ));
            return;
        }

        if (!validDayNumber) {
            return;
        }

        int startMinute = absoluteMinute(item.getDayNumber(), startTime, 0);
        int endMinute = absoluteMinute(item.getDayNumber(), endTime, endDayOffset);

        if (validTravelDays && item.getDayNumber() + endDayOffset > travelDays) {
            issues.add(issue(
                    "END_AFTER_TRIP",
                    "WARNING",
                    itemKey,
                    null,
                    "节点结束时间超出了本次旅行日期范围"
            ));
        }

        timedItems.add(new TimedDraftItem(
                itemKey,
                item.getDayNumber(),
                item.getItemOrder(),
                startMinute,
                endMinute,
                item.getTravelTimeFromPrev()
        ));
    }

    private void validateTimeline(
            List<TimedDraftItem> timedItems,
            List<TravelValidationIssue> issues) {

        List<TimedDraftItem> chronologicalItems = new ArrayList<>(timedItems);
        chronologicalItems.sort(Comparator
                .comparingInt(TimedDraftItem::startMinute)
                .thenComparing(item -> item.itemOrder() == null
                        ? Integer.MAX_VALUE
                        : item.itemOrder()));

        TimedDraftItem activeItem = null;
        for (TimedDraftItem current : chronologicalItems) {
            if (activeItem != null && current.startMinute() < activeItem.endMinute()) {
                issues.add(issue(
                        "TIME_CONFLICT",
                        "ERROR",
                        current.itemKey(),
                        activeItem.itemKey(),
                        "两个行程节点的时间发生重叠"
                ));
            }

            if (activeItem == null || current.endMinute() > activeItem.endMinute()) {
                activeItem = current;
            }
        }

        List<TimedDraftItem> orderedItems = new ArrayList<>(timedItems);
        orderedItems.sort(Comparator
                .comparing(TimedDraftItem::dayNumber)
                .thenComparing(item -> item.itemOrder() == null
                        ? Integer.MAX_VALUE
                        : item.itemOrder()));

        TimedDraftItem previousByOrder = null;
        for (TimedDraftItem current : orderedItems) {
            if (previousByOrder == null
                    || !current.dayNumber().equals(previousByOrder.dayNumber())) {
                previousByOrder = current;
                continue;
            }

            if (current.startMinute() < previousByOrder.startMinute()) {
                issues.add(issue(
                        "ITEM_ORDER_TIME_CONFLICT",
                        "ERROR",
                        current.itemKey(),
                        previousByOrder.itemKey(),
                        "节点时间顺序与 itemOrder 不一致"
                ));
            } else if (current.startMinute() >= previousByOrder.endMinute()
                    && current.travelTimeFromPrev() != null
                    && current.travelTimeFromPrev() >= 0
                    && current.startMinute()
                    < previousByOrder.endMinute() + current.travelTimeFromPrev()) {
                issues.add(issue(
                        "ROUTE_TIME_CONFLICT",
                        "ERROR",
                        current.itemKey(),
                        previousByOrder.itemKey(),
                        "两个节点之间预留的时间不足以完成交通移动"
                ));
            }

            previousByOrder = current;
        }
    }

    private int absoluteMinute(int dayNumber, LocalTime time, int dayOffset) {
        return (dayNumber - 1 + dayOffset) * MINUTES_PER_DAY
                + time.getHour() * 60
                + time.getMinute();
    }

    private boolean isNegative(Integer value) {
        return value != null && value < 0;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String itemKey(TravelPlanDraftItem item) {
        if (!isBlank(item.getDraftItemKey())) {
            return item.getDraftItemKey();
        }
        if (item.getDayNumber() == null && item.getItemOrder() == null) {
            return null;
        }
        return "D" + valueOrQuestionMark(item.getDayNumber())
                + "-I" + valueOrQuestionMark(item.getItemOrder());
    }

    private String valueOrQuestionMark(Integer value) {
        return value == null ? "?" : value.toString();
    }

    private TravelValidationIssue issue(
            String code,
            String severity,
            String draftItemKey,
            String relatedDraftItemKey,
            String message) {
        return new TravelValidationIssue(
                code,
                severity,
                draftItemKey,
                relatedDraftItemKey,
                message
        );
    }

    private record TimedDraftItem(
            String itemKey,
            Integer dayNumber,
            Integer itemOrder,
            int startMinute,
            int endMinute,
            Integer travelTimeFromPrev) {
    }
}
