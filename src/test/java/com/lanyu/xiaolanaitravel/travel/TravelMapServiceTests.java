package com.lanyu.xiaolanaitravel.travel;

import com.lanyu.xiaolanaitravel.amap.dto.AmapPoiItem;
import com.lanyu.xiaolanaitravel.amap.dto.AmapPoiPhoto;
import com.lanyu.xiaolanaitravel.amap.dto.AmapRouteResult;
import com.lanyu.xiaolanaitravel.amap.dto.AmapTravelMode;
import com.lanyu.xiaolanaitravel.amap.service.AmapService;
import com.lanyu.xiaolanaitravel.travel.entity.TravelPlan;
import com.lanyu.xiaolanaitravel.travel.entity.TravelPlanItem;
import com.lanyu.xiaolanaitravel.travel.mapper.TravelPlanItemMapper;
import com.lanyu.xiaolanaitravel.travel.service.TravelMapService;
import com.lanyu.xiaolanaitravel.travel.service.TravelPlanService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TravelMapServiceTests {

    @Test
    void locationMatchReturnsPhotoFromSamePoiRequest() {
        TravelPlanService planService = mock(TravelPlanService.class);
        TravelPlan plan = new TravelPlan();
        plan.setDestination("厦门");
        when(planService.getMyPlanById(7L, 12L)).thenReturn(plan);

        TravelPlanItemMapper mapper = mock(TravelPlanItemMapper.class);
        TravelPlanItem item = item(2L, 1, 1, "白城沙滩", null, null);
        when(mapper.selectOne(any())).thenReturn(item);
        when(mapper.updateById(item)).thenReturn(1);

        AmapPoiItem poi = new AmapPoiItem();
        poi.setId("B001");
        poi.setName("白城沙滩");
        poi.setAddress("思明区大学路");
        poi.setLocation("118.090000,24.430000");
        poi.setCitycode("0592");
        AmapPoiPhoto photo = new AmapPoiPhoto();
        photo.setUrl("https://example.test/baicheng.jpg");
        poi.setPhotos(List.of(photo));

        AmapService amapService = mock(AmapService.class);
        when(amapService.searchPois("白城沙滩", "厦门", 3))
                .thenReturn(List.of(poi));

        var response = new TravelMapService(
                planService,
                mapper,
                amapService
        ).resolveItemLocation(7L, 12L, 2L, false);

        assertEquals("https://example.test/baicheng.jpg", response.imageUrl());
        assertEquals("白城沙滩", response.poiName());
        verify(amapService).searchPois("白城沙滩", "厦门", 3);
    }

    @Test
    void calculatesSelectedModeBetweenTwoLocatedItems() {
        TravelPlanService planService = mock(TravelPlanService.class);
        when(planService.getMyPlanById(7L, 12L)).thenReturn(new TravelPlan());

        TravelPlanItem previous = item(
                1L, 1, 1, "沙坡尾", "118.087000", "24.438000"
        );
        TravelPlanItem current = item(
                2L, 1, 2, "白城沙滩", "118.091000", "24.432000"
        );
        previous.setCityCode("0592");
        current.setCityCode("0592");

        TravelPlanItemMapper mapper = mock(TravelPlanItemMapper.class);
        when(mapper.selectOne(any())).thenReturn(current, previous);
        when(mapper.updateById(current)).thenReturn(1);

        AmapService amapService = mock(AmapService.class);
        when(amapService.calculateRoute(
                previous.getLongitude(), previous.getLatitude(),
                current.getLongitude(), current.getLatitude(),
                "0592", "0592", AmapTravelMode.BICYCLING
        )).thenReturn(new AmapRouteResult(
                AmapTravelMode.BICYCLING,
                1800,
                720
        ));

        var response = new TravelMapService(
                planService,
                mapper,
                amapService
        ).calculateRouteFromPrevious(
                7L, 12L, 2L, AmapTravelMode.BICYCLING, false
        );

        assertEquals(AmapTravelMode.BICYCLING, response.mode());
        assertEquals(1800, response.distanceMeters());
        assertEquals(12, response.durationMinutes());
        assertEquals("BICYCLING", current.getTransportMode());
        assertEquals(1800, current.getDistanceFromPrev());
    }

    private TravelPlanItem item(
            Long id,
            int day,
            int order,
            String name,
            String longitude,
            String latitude) {
        TravelPlanItem item = new TravelPlanItem();
        item.setId(id);
        item.setPlanId(12L);
        item.setDayNumber(day);
        item.setItemOrder(order);
        item.setPlaceName(name);
        item.setLongitude(longitude == null ? null : new BigDecimal(longitude));
        item.setLatitude(latitude == null ? null : new BigDecimal(latitude));
        return item;
    }
}
