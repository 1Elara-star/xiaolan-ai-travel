package com.lanyu.xiaolanaitravel.ai;

import com.lanyu.xiaolanaitravel.ai.dto.AiTravelDay;
import com.lanyu.xiaolanaitravel.ai.dto.AiTravelItem;
import com.lanyu.xiaolanaitravel.ai.dto.AiTravelPlanResponse;
import com.lanyu.xiaolanaitravel.ai.service.AiTravelPlanService;
import com.lanyu.xiaolanaitravel.travel.entity.TravelPlan;
import com.lanyu.xiaolanaitravel.travel.entity.TravelPlanItem;
import com.lanyu.xiaolanaitravel.travel.mapper.TravelPlanItemMapper;
import com.lanyu.xiaolanaitravel.travel.service.TravelPlanService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiTravelPlanServiceTests {

    @Test
    void savesCrossDayItemWithEndDayOffset() {
        TravelPlanService planService = mock(TravelPlanService.class);
        TravelPlanItemMapper itemMapper = mock(TravelPlanItemMapper.class);

        TravelPlan plan = new TravelPlan();
        plan.setTravelDays(2);
        when(planService.getMyPlanById(1L, 2L)).thenReturn(plan);
        when(itemMapper.selectCount(any())).thenReturn(0L);
        when(itemMapper.insert(any(TravelPlanItem.class))).thenReturn(1);

        AiTravelItem aiItem = new AiTravelItem();
        aiItem.setPlaceName("夜班列车");
        aiItem.setItemType("OTHER");
        aiItem.setStartTime("23:30");
        aiItem.setEndTime("06:30");
        aiItem.setEndDayOffset(1);

        AiTravelDay day = new AiTravelDay();
        day.setDayNumber(1);
        day.setItems(List.of(aiItem));

        AiTravelPlanResponse aiPlan = new AiTravelPlanResponse();
        aiPlan.setDays(List.of(day));

        new AiTravelPlanService(planService, itemMapper)
                .saveGeneratedPlan(1L, 2L, aiPlan);

        ArgumentCaptor<TravelPlanItem> captor =
                ArgumentCaptor.forClass(TravelPlanItem.class);
        verify(itemMapper).insert(captor.capture());

        TravelPlanItem saved = captor.getValue();
        assertEquals(LocalTime.of(23, 30), saved.getStartTime());
        assertEquals(LocalTime.of(6, 30), saved.getEndTime());
        assertEquals(1, saved.getEndDayOffset());
    }
}
