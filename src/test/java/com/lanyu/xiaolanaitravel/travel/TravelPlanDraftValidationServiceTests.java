package com.lanyu.xiaolanaitravel.travel;

import com.lanyu.xiaolanaitravel.travel.dto.TravelPlanDraft;
import com.lanyu.xiaolanaitravel.travel.dto.TravelPlanDraftItem;
import com.lanyu.xiaolanaitravel.travel.dto.TravelValidationIssue;
import com.lanyu.xiaolanaitravel.travel.service.TravelPlanDraftValidationService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TravelPlanDraftValidationServiceTests {

    private final TravelPlanDraftValidationService service =
            new TravelPlanDraftValidationService();

    @Test
    void validDraftHasNoIssues() {
        TravelPlanDraftItem first = item(1, 1, "ATTRACTION", "鼓浪屿",
                LocalTime.of(9, 0), LocalTime.of(11, 0), 0);
        TravelPlanDraftItem second = item(1, 2, "FOOD", "午餐",
                LocalTime.of(12, 0), LocalTime.of(13, 0), 0);

        List<TravelValidationIssue> issues = service.validate(draft(1, first, second));

        assertTrue(issues.isEmpty());
    }

    @Test
    void reportsInvalidStructureWithoutThrowing() {
        TravelPlanDraftItem invalid = item(3, 0, "UNKNOWN", " ",
                null, null, 0);

        List<TravelValidationIssue> issues = service.validate(draft(2, invalid));

        assertHasCode(issues, "INVALID_DAY_NUMBER");
        assertHasCode(issues, "INVALID_ITEM_ORDER");
        assertHasCode(issues, "MISSING_PLACE_NAME");
        assertHasCode(issues, "INVALID_ITEM_TYPE");
        assertHasCode(issues, "EMPTY_DAY");
    }

    @Test
    void reportsSameDayTimeOverlap() {
        TravelPlanDraftItem first = item(1, 1, "ATTRACTION", "鼓浪屿",
                LocalTime.of(9, 0), LocalTime.of(11, 0), 0);
        TravelPlanDraftItem second = item(1, 2, "ATTRACTION", "菽庄花园",
                LocalTime.of(10, 30), LocalTime.of(12, 0), 0);

        List<TravelValidationIssue> issues = service.validate(draft(1, first, second));

        TravelValidationIssue issue = find(issues, "TIME_CONFLICT");
        assertEquals("D1-I2", issue.draftItemKey());
        assertEquals("D1-I1", issue.relatedDraftItemKey());
    }

    @Test
    void acceptsOvernightSleepWithoutFalseConflict() {
        TravelPlanDraftItem sleep = item(1, 1, "HOTEL", "酒店休息",
                LocalTime.of(22, 30), LocalTime.of(8, 0), 1);
        TravelPlanDraftItem breakfast = item(2, 1, "FOOD", "早餐",
                LocalTime.of(8, 30), LocalTime.of(9, 0), 0);

        List<TravelValidationIssue> issues = service.validate(draft(2, sleep, breakfast));

        assertTrue(issues.isEmpty());
    }

    @Test
    void overnightItemCanConflictWithNextDayItem() {
        TravelPlanDraftItem sleep = item(1, 1, "HOTEL", "酒店休息",
                LocalTime.of(22, 30), LocalTime.of(8, 0), 1);
        TravelPlanDraftItem earlyActivity = item(2, 1, "ATTRACTION", "日出",
                LocalTime.of(7, 30), LocalTime.of(9, 0), 0);

        List<TravelValidationIssue> issues = service.validate(draft(2, sleep, earlyActivity));

        assertHasCode(issues, "TIME_CONFLICT");
    }

    @Test
    void reportsInsufficientTimeForKnownRoute() {
        TravelPlanDraftItem first = item(1, 1, "ATTRACTION", "鼓浪屿",
                LocalTime.of(9, 0), LocalTime.of(10, 0), 0);
        TravelPlanDraftItem second = item(1, 2, "ATTRACTION", "植物园",
                LocalTime.of(10, 20), LocalTime.of(12, 0), 0);
        second.setTravelTimeFromPrev(35);

        List<TravelValidationIssue> issues = service.validate(draft(1, first, second));

        assertHasCode(issues, "ROUTE_TIME_CONFLICT");
    }

    @Test
    void reportsTimeOrderThatDisagreesWithItemOrder() {
        TravelPlanDraftItem first = item(1, 1, "ATTRACTION", "植物园",
                LocalTime.of(14, 0), LocalTime.of(16, 0), 0);
        TravelPlanDraftItem second = item(1, 2, "ATTRACTION", "鼓浪屿",
                LocalTime.of(9, 0), LocalTime.of(11, 0), 0);

        List<TravelValidationIssue> issues = service.validate(draft(1, first, second));

        assertHasCode(issues, "ITEM_ORDER_TIME_CONFLICT");
    }

    @Test
    void reportsIncompleteMatchedPoiCoordinates() {
        TravelPlanDraftItem item = item(1, 1, "ATTRACTION", "鼓浪屿",
                null, null, 0);
        item.setPoiId("B001");
        item.setLongitude(new BigDecimal("118.066"));

        List<TravelValidationIssue> issues = service.validate(draft(1, item));

        assertHasCode(issues, "INCOMPLETE_COORDINATES");
    }

    @Test
    void nullDraftIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.validate(null));
    }

    private TravelPlanDraft draft(int travelDays, TravelPlanDraftItem... items) {
        TravelPlanDraft draft = new TravelPlanDraft();
        draft.setTravelDays(travelDays);
        draft.setItems(new ArrayList<>(List.of(items)));
        return draft;
    }

    private TravelPlanDraftItem item(
            int dayNumber,
            int itemOrder,
            String itemType,
            String placeName,
            LocalTime startTime,
            LocalTime endTime,
            int endDayOffset) {
        TravelPlanDraftItem item = new TravelPlanDraftItem();
        item.setDraftItemKey("D" + dayNumber + "-I" + itemOrder);
        item.setDayNumber(dayNumber);
        item.setItemOrder(itemOrder);
        item.setItemType(itemType);
        item.setPlaceName(placeName);
        item.setStartTime(startTime);
        item.setEndTime(endTime);
        item.setEndDayOffset(endDayOffset);
        return item;
    }

    private void assertHasCode(List<TravelValidationIssue> issues, String code) {
        find(issues, code);
    }

    private TravelValidationIssue find(
            List<TravelValidationIssue> issues,
            String code) {
        return issues.stream()
                .filter(issue -> code.equals(issue.code()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("没有找到校验问题：" + code));
    }
}
