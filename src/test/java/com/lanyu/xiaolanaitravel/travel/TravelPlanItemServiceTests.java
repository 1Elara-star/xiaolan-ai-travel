package com.lanyu.xiaolanaitravel.travel;

import com.lanyu.xiaolanaitravel.travel.dto.TravelPlanItemRequest;
import com.lanyu.xiaolanaitravel.travel.entity.TravelPlan;
import com.lanyu.xiaolanaitravel.travel.mapper.TravelPlanItemMapper;
import com.lanyu.xiaolanaitravel.travel.service.TravelPlanItemService;
import com.lanyu.xiaolanaitravel.travel.service.TravelPlanService;
import com.lanyu.xiaolanaitravel.explore.mapper.AttractionMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
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

    private TravelPlanMapperFixture fixture(int travelDays) {
        TravelPlanService planService = mock(TravelPlanService.class);
        TravelPlan plan = new TravelPlan();
        plan.setTravelDays(travelDays);
        when(planService.getMyPlanById(1L, 2L)).thenReturn(plan);
        return new TravelPlanMapperFixture(
                new TravelPlanItemService(mock(TravelPlanItemMapper.class), planService,
                        mock(AttractionMapper.class)));
    }

    private TravelPlanItemRequest request(int day, LocalTime start, LocalTime end) {
        return new TravelPlanItemRequest(day, 1, "ATTRACTION", null, "测试地点", null,
                null, null, start, end, null, null, null, null);
    }

    private record TravelPlanMapperFixture(TravelPlanItemService service) {
    }
}
