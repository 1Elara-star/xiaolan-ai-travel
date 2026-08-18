package com.lanyu.xiaolanaitravel.travel;

import com.lanyu.xiaolanaitravel.travel.dto.TravelPlanItemRequest;
import com.lanyu.xiaolanaitravel.travel.entity.TravelPlan;
import com.lanyu.xiaolanaitravel.travel.entity.TravelPlanItem;
import com.lanyu.xiaolanaitravel.travel.mapper.TravelPlanItemMapper;
import com.lanyu.xiaolanaitravel.travel.service.TravelPlanItemService;
import com.lanyu.xiaolanaitravel.travel.service.TravelPlanService;
import com.lanyu.xiaolanaitravel.explore.mapper.AttractionMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
                        mock(AttractionMapper.class)));
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
