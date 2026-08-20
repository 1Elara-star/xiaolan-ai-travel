package com.lanyu.xiaolanaitravel.travel;

import com.lanyu.xiaolanaitravel.travel.dto.TravelPlanItemRequest;
import com.lanyu.xiaolanaitravel.travel.dto.TravelPlanDraftItem;
import com.lanyu.xiaolanaitravel.travel.entity.TravelPlan;
import com.lanyu.xiaolanaitravel.travel.entity.TravelPlanItem;
import com.lanyu.xiaolanaitravel.travel.mapper.TravelPlanItemMapper;
import com.lanyu.xiaolanaitravel.travel.service.TravelDistanceService;
import com.lanyu.xiaolanaitravel.travel.service.TravelPlanItemService;
import com.lanyu.xiaolanaitravel.travel.service.TravelPlanService;
import com.lanyu.xiaolanaitravel.explore.mapper.AttractionMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

class TravelPlanItemServiceTests {

    @Test
    void rejectsNodeOutsidePlanDayRange() {
        TravelPlanMapperFixture fixture = fixture(3);
        TravelPlanItemRequest request = request(4, LocalTime.of(9, 0), LocalTime.of(10, 0));

        assertThrows(ResponseStatusException.class,
                () -> fixture.service.create(1L, 2L, request));
    }

    @Test
    void rejectsEndTimeBeforeStartTime() {
        TravelPlanMapperFixture fixture = fixture(3);
        TravelPlanItemRequest request = request(1, LocalTime.of(10, 0), LocalTime.of(9, 0));

        assertThrows(ResponseStatusException.class,
                () -> fixture.service.create(1L, 2L, request));
    }

    @Test
    void acceptsEndTimeOnNextDay() {
        TravelPlanMapperFixture fixture = fixture(3);
        TravelPlanItemRequest request = request(
                1, LocalTime.of(23, 30), LocalTime.of(6, 30), 1);

        var response = fixture.service.create(1L, 2L, request);

        assertEquals(1, response.endDayOffset());
        assertEquals(LocalTime.of(23, 30), response.startTime());
        assertEquals(LocalTime.of(6, 30), response.endTime());
    }

    @Test
    void addsSelectedDraftItemsWithoutDeletingExistingNodes() {
        TravelPlanService planService = mock(TravelPlanService.class);
        TravelPlan plan = new TravelPlan();
        plan.setTravelDays(2);
        when(planService.getMyPlanById(1L, 2L)).thenReturn(plan);

        TravelPlanItemMapper mapper = mock(TravelPlanItemMapper.class);
        TravelPlanItem existing = new TravelPlanItem();
        existing.setId(8L);
        existing.setPlanId(2L);
        existing.setDayNumber(1);
        existing.setItemOrder(2);
        existing.setItemType("ATTRACTION");
        existing.setPlaceName("已有节点");

        AtomicReference<TravelPlanItem> inserted = new AtomicReference<>();
        AtomicInteger listCalls = new AtomicInteger();
        when(mapper.selectList(any())).thenAnswer(invocation -> {
            if (listCalls.getAndIncrement() == 0) {
                return List.of(existing);
            }
            return List.of(existing, inserted.get());
        });
        when(mapper.insert(any(TravelPlanItem.class))).thenAnswer(invocation -> {
            TravelPlanItem item = invocation.getArgument(0);
            item.setId(9L);
            inserted.set(item);
            return 1;
        });

        TravelPlanDraftItem selected = new TravelPlanDraftItem();
        selected.setDraftItemKey("D1-I4");
        selected.setDayNumber(1);
        selected.setItemOrder(4);
        selected.setItemType("ATTRACTION");
        selected.setPlaceName("候选节点");
        selected.setEndDayOffset(0);

        TravelPlanItemService service = new TravelPlanItemService(
                mapper,
                planService,
                mock(AttractionMapper.class),
                new TravelDistanceService()
        );
        var result = service.addFromDraft(
                1L,
                2L,
                List.of(selected)
        );

        assertEquals(2, result.size());
        assertEquals(3, inserted.get().getItemOrder());
        assertEquals("候选节点", inserted.get().getPlaceName());
        verify(mapper, never()).delete(any());
    }

    @Test
    void previewsStraightLineDistanceForAdjacentLocatedItemsOnSameDay() {
        TravelPlanItem first = locatedItem(
                1L, 1, 1, "沙坡尾", "118.087854", "24.437990");
        TravelPlanItem second = locatedItem(
                2L, 1, 2, "中山路步行街", "118.078336", "24.451212");

        TravelPlanItemService service = responseService(List.of(first, second));
        var responses = service.getMyPlanItemResponses(1L, 2L);

        assertNull(responses.get(0).straightLineDistanceFromPrev());
        assertTrue(responses.get(1).straightLineDistanceFromPrev() > 0);
        assertNull(responses.get(1).transportMode());
        assertNull(responses.get(1).distanceFromPrev());
    }

    @Test
    void doesNotPreviewDistanceAcrossDifferentDays() {
        TravelPlanItem first = locatedItem(
                1L, 1, 1, "沙坡尾", "118.087854", "24.437990");
        TravelPlanItem second = locatedItem(
                2L, 2, 1, "鼓浪屿", "118.066500", "24.447100");

        TravelPlanItemService service = responseService(List.of(first, second));
        var responses = service.getMyPlanItemResponses(1L, 2L);

        assertNull(responses.get(0).straightLineDistanceFromPrev());
        assertNull(responses.get(1).straightLineDistanceFromPrev());
    }

    @Test
    void doesNotConnectAcrossAnUnlocatedFormalItineraryItem() {
        TravelPlanItem first = locatedItem(
                1L, 1, 1, "沙坡尾", "118.087854", "24.437990");
        TravelPlanItem unlocated = locatedItem(
                2L, 1, 2, "午餐", null, null);
        TravelPlanItem third = locatedItem(
                3L, 1, 3, "中山路步行街", "118.078336", "24.451212");

        TravelPlanItemService service = responseService(
                List.of(first, unlocated, third));
        var responses = service.getMyPlanItemResponses(1L, 2L);

        assertNull(responses.get(1).straightLineDistanceFromPrev());
        assertNull(responses.get(2).straightLineDistanceFromPrev());
    }

    private TravelPlanMapperFixture fixture(int travelDays) {
        TravelPlanService planService = mock(TravelPlanService.class);
        TravelPlan plan = new TravelPlan();
        plan.setTravelDays(travelDays);
        when(planService.getMyPlanById(1L, 2L)).thenReturn(plan);

        TravelPlanItemMapper mapper = mock(TravelPlanItemMapper.class);
        AtomicReference<TravelPlanItem> storedItem = new AtomicReference<>();
        when(mapper.insert(any(TravelPlanItem.class))).thenAnswer(invocation -> {
            var item = (TravelPlanItem) invocation.getArgument(0);
            item.setId(10L);
            storedItem.set(item);
            return 1;
        });
        when(mapper.selectById(10L)).thenAnswer(invocation -> storedItem.get());

        return new TravelPlanMapperFixture(
                new TravelPlanItemService(mapper, planService,
                        mock(AttractionMapper.class),
                        new TravelDistanceService()));
    }

    private TravelPlanItemService responseService(List<TravelPlanItem> items) {
        TravelPlanService planService = mock(TravelPlanService.class);
        TravelPlan plan = new TravelPlan();
        plan.setTravelDays(5);
        when(planService.getMyPlanById(1L, 2L)).thenReturn(plan);

        TravelPlanItemMapper mapper = mock(TravelPlanItemMapper.class);
        when(mapper.selectList(any())).thenReturn(items);

        return new TravelPlanItemService(
                mapper,
                planService,
                mock(AttractionMapper.class),
                new TravelDistanceService()
        );
    }

    private TravelPlanItem locatedItem(
            Long id,
            int dayNumber,
            int itemOrder,
            String placeName,
            String longitude,
            String latitude) {
        TravelPlanItem item = new TravelPlanItem();
        item.setId(id);
        item.setPlanId(2L);
        item.setDayNumber(dayNumber);
        item.setItemOrder(itemOrder);
        item.setItemType("ATTRACTION");
        item.setPlaceName(placeName);
        item.setLongitude(longitude == null ? null : new BigDecimal(longitude));
        item.setLatitude(latitude == null ? null : new BigDecimal(latitude));
        return item;
    }

    private TravelPlanItemRequest request(int day, LocalTime start, LocalTime end) {
        return request(day, start, end, 0);
    }

    private TravelPlanItemRequest request(
            int day, LocalTime start, LocalTime end, Integer endDayOffset) {
        return new TravelPlanItemRequest(day, 1, "ATTRACTION", null, "测试地点", null,
                null, null, start, end, endDayOffset, null, null, null, null);
    }

    private record TravelPlanMapperFixture(TravelPlanItemService service) {
    }
}
