package com.lanyu.xiaolanaitravel.travel;

import com.lanyu.xiaolanaitravel.amap.dto.AmapPoiItem;
import com.lanyu.xiaolanaitravel.amap.dto.AmapPoiPhoto;
import com.lanyu.xiaolanaitravel.amap.service.AmapService;
import com.lanyu.xiaolanaitravel.travel.dto.TravelPlanDraft;
import com.lanyu.xiaolanaitravel.travel.dto.TravelPlanDraftItem;
import com.lanyu.xiaolanaitravel.travel.service.TravelDraftMapService;
import com.lanyu.xiaolanaitravel.travel.service.TravelDistanceService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TravelDraftMapServiceTests {

    @Test
    void attractionReceivesPoiLocationFields() {
        AmapService amapService = mock(AmapService.class);
        TravelPlanDraftItem item = draftItem("ATTRACTION", "夫子庙");
        TravelPlanDraft draft = draft("南京", item);
        AmapPoiItem poi = poi("B001", "夫子庙", "贡院街152号",
                "118.796877,32.020694", "025");
        AmapPoiPhoto photo = new AmapPoiPhoto();
        photo.setUrl("https://example.test/fuzimiao.jpg");
        poi.setPhotos(List.of(photo));
        when(amapService.searchPois("夫子庙", "南京", 3)).thenReturn(List.of(poi));

        TravelPlanDraft result = service(amapService, mock(TravelDistanceService.class))
                .enrichLocations(draft);

        assertSame(draft, result);
        assertPoiFields(item, "夫子庙");
        assertEquals("https://example.test/fuzimiao.jpg", item.getImageUrl());
        verify(amapService).searchPois("夫子庙", "南京", 3);
    }

    @Test
    void genericPlaceNamesDoNotCallAmap() {
        AmapService amapService = mock(AmapService.class);
        TravelPlanDraft draft = draft(
                "南京",
                draftItem("FOOD", "午餐"),
                draftItem("HOTEL", "待推荐酒店"),
                draftItem("HOTEL", "住宿休息")
        );

        service(amapService, mock(TravelDistanceService.class)).enrichLocations(draft);

        verifyNoInteractions(amapService);
    }

    @Test
    void duplicatePlacesUseOneAmapRequestAndFillBothItems() {
        AmapService amapService = mock(AmapService.class);
        TravelPlanDraftItem first = draftItem("ATTRACTION", "夫子庙");
        TravelPlanDraftItem second = draftItem("ATTRACTION", "夫子庙");
        TravelPlanDraft draft = draft("南京", first, second);
        AmapPoiItem poi = poi("B001", "南京夫子庙", "贡院街152号",
                "118.796877,32.020694", "025");
        when(amapService.searchPois("夫子庙", "南京", 3)).thenReturn(List.of(poi));

        service(amapService, mock(TravelDistanceService.class)).enrichLocations(draft);

        verify(amapService, times(1)).searchPois("夫子庙", "南京", 3);
        assertPoiFields(first, "南京夫子庙");
        assertPoiFields(second, "南京夫子庙");
    }

    @Test
    void emptyPoiResultKeepsCoordinatesNull() {
        AmapService amapService = mock(AmapService.class);
        TravelPlanDraftItem item = draftItem("ATTRACTION", "不存在的地点");
        TravelPlanDraft draft = draft("南京", item);
        when(amapService.searchPois("不存在的地点", "南京", 3)).thenReturn(List.of());

        TravelPlanDraft result = service(amapService, mock(TravelDistanceService.class))
                .enrichLocations(draft);

        assertSame(draft, result);
        assertNull(item.getLongitude());
        assertNull(item.getLatitude());
        verify(amapService).searchPois("不存在的地点", "南京", 3);
    }

    @Test
    void calculatesDistanceBetweenTwoLocatedItemsOnSameDay() {
        TravelDistanceService distanceService = mock(TravelDistanceService.class);
        TravelPlanDraftItem first = locatedItem(1, 1, "A", "118.790000", "32.010000");
        TravelPlanDraftItem second = locatedItem(1, 2, "B", "118.800000", "32.020000");
        TravelPlanDraft draft = draft("南京", first, second);
        when(distanceService.calculateStraightLineDistanceMeters(
                first.getLongitude(), first.getLatitude(),
                second.getLongitude(), second.getLatitude())).thenReturn(1460);

        TravelPlanDraft result = service(mock(AmapService.class), distanceService)
                .enrichStraightLineDistances(draft);

        assertSame(draft, result);
        assertNull(first.getStraightLineDistanceFromPrev());
        assertTrue(second.getStraightLineDistanceFromPrev() > 0);
        assertEquals(1460, second.getStraightLineDistanceFromPrev());
        verify(distanceService).calculateStraightLineDistanceMeters(
                first.getLongitude(), first.getLatitude(),
                second.getLongitude(), second.getLatitude());
    }

    @Test
    void itemWithoutCoordinatesDoesNotBreakPreviousLocatedItem() {
        TravelDistanceService distanceService = mock(TravelDistanceService.class);
        TravelPlanDraftItem first = locatedItem(1, 1, "A", "118.790000", "32.010000");
        TravelPlanDraftItem meal = orderedItem(1, 2, "FOOD", "午餐");
        TravelPlanDraftItem second = locatedItem(1, 3, "B", "118.800000", "32.020000");
        TravelPlanDraft draft = draft("南京", first, meal, second);
        when(distanceService.calculateStraightLineDistanceMeters(
                first.getLongitude(), first.getLatitude(),
                second.getLongitude(), second.getLatitude())).thenReturn(1460);

        service(mock(AmapService.class), distanceService)
                .enrichStraightLineDistances(draft);

        assertNull(first.getStraightLineDistanceFromPrev());
        assertNull(meal.getStraightLineDistanceFromPrev());
        assertEquals(1460, second.getStraightLineDistanceFromPrev());
        verify(distanceService).calculateStraightLineDistanceMeters(
                first.getLongitude(), first.getLatitude(),
                second.getLongitude(), second.getLatitude());
    }

    @Test
    void doesNotCalculateDistanceAcrossDifferentDays() {
        TravelDistanceService distanceService = mock(TravelDistanceService.class);
        TravelPlanDraftItem dayOne = locatedItem(1, 1, "A", "118.790000", "32.010000");
        TravelPlanDraftItem dayTwo = locatedItem(2, 1, "B", "118.800000", "32.020000");
        TravelPlanDraft draft = draft("南京", dayOne, dayTwo);

        service(mock(AmapService.class), distanceService)
                .enrichStraightLineDistances(draft);

        assertNull(dayOne.getStraightLineDistanceFromPrev());
        assertNull(dayTwo.getStraightLineDistanceFromPrev());
        verifyNoInteractions(distanceService);
    }

    @Test
    void clearsStaleDistancesBeforeRecalculation() {
        TravelDistanceService distanceService = mock(TravelDistanceService.class);
        TravelPlanDraftItem first = locatedItem(1, 1, "A", "118.790000", "32.010000");
        TravelPlanDraftItem second = locatedItem(1, 2, "B", "118.800000", "32.020000");
        first.setStraightLineDistanceFromPrev(999);
        second.setStraightLineDistanceFromPrev(888);
        TravelPlanDraft draft = draft("南京", first, second);
        when(distanceService.calculateStraightLineDistanceMeters(
                first.getLongitude(), first.getLatitude(),
                second.getLongitude(), second.getLatitude())).thenReturn(1460);

        service(mock(AmapService.class), distanceService)
                .enrichStraightLineDistances(draft);

        assertNull(first.getStraightLineDistanceFromPrev());
        assertEquals(1460, second.getStraightLineDistanceFromPrev());
    }

    private TravelPlanDraft draft(String destination, TravelPlanDraftItem... items) {
        TravelPlanDraft draft = new TravelPlanDraft();
        draft.setDestination(destination);
        draft.setItems(List.of(items));
        return draft;
    }

    private TravelPlanDraftItem draftItem(String itemType, String placeName) {
        TravelPlanDraftItem item = new TravelPlanDraftItem();
        item.setItemType(itemType);
        item.setPlaceName(placeName);
        return item;
    }

    private TravelPlanDraftItem orderedItem(
            int dayNumber,
            int itemOrder,
            String itemType,
            String placeName) {
        TravelPlanDraftItem item = draftItem(itemType, placeName);
        item.setDayNumber(dayNumber);
        item.setItemOrder(itemOrder);
        return item;
    }

    private TravelPlanDraftItem locatedItem(
            int dayNumber,
            int itemOrder,
            String placeName,
            String longitude,
            String latitude) {
        TravelPlanDraftItem item = orderedItem(dayNumber, itemOrder, "ATTRACTION", placeName);
        item.setLongitude(new BigDecimal(longitude));
        item.setLatitude(new BigDecimal(latitude));
        return item;
    }

    private TravelDraftMapService service(
            AmapService amapService,
            TravelDistanceService distanceService) {
        return new TravelDraftMapService(amapService, distanceService);
    }

    private void assertPoiFields(TravelPlanDraftItem item, String matchedPoiName) {
        assertEquals("B001", item.getPoiId());
        assertEquals(matchedPoiName, item.getMatchedPoiName());
        assertEquals("贡院街152号", item.getAddress());
        assertEquals(new BigDecimal("118.796877"), item.getLongitude());
        assertEquals(new BigDecimal("32.020694"), item.getLatitude());
        assertEquals("025", item.getCityCode());
    }

    private AmapPoiItem poi(
            String id,
            String name,
            String address,
            String location,
            String cityCode) {
        AmapPoiItem poi = new AmapPoiItem();
        poi.setId(id);
        poi.setName(name);
        poi.setAddress(address);
        poi.setLocation(location);
        poi.setCitycode(cityCode);
        return poi;
    }
}
