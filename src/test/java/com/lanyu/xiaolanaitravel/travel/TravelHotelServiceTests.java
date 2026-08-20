package com.lanyu.xiaolanaitravel.travel;

import com.lanyu.xiaolanaitravel.ai.dto.FlyAiHotelData;
import com.lanyu.xiaolanaitravel.ai.dto.FlyAiHotelItem;
import com.lanyu.xiaolanaitravel.ai.dto.FlyAiHotelResponse;
import com.lanyu.xiaolanaitravel.ai.service.FlyAiService;
import com.lanyu.xiaolanaitravel.travel.dto.HotelSearchCriteria;
import com.lanyu.xiaolanaitravel.travel.dto.HotelLocationType;
import com.lanyu.xiaolanaitravel.travel.entity.TravelPlan;
import com.lanyu.xiaolanaitravel.travel.service.HotelRecommendationService;
import com.lanyu.xiaolanaitravel.travel.service.TravelHotelService;
import com.lanyu.xiaolanaitravel.travel.service.TravelPlanService;
import com.lanyu.xiaolanaitravel.user.entity.UserProfile;
import com.lanyu.xiaolanaitravel.user.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TravelHotelServiceTests {

    @Mock
    private TravelPlanService travelPlanService;
    @Mock
    private FlyAiService flyAiService;
    @Mock
    private UserProfileService userProfileService;

    private TravelHotelService travelHotelService;

    @BeforeEach
    void setUp() {
        travelHotelService = new TravelHotelService(
                travelPlanService,
                flyAiService,
                userProfileService,
                new HotelRecommendationService());
    }

    @Test
    void shouldUseLandmarkAndPriceFiltersAndApplyProfileRanking() {
        TravelPlan plan = new TravelPlan();
        plan.setDestination("成都");
        when(travelPlanService.getMyPlanById(7L, 10L)).thenReturn(plan);

        UserProfile profile = new UserProfile();
        profile.setBudgetPreference("在意性价比");
        profile.setAccommodationPreference("干净的酒店");
        when(userProfileService.getProfile(7L)).thenReturn(profile);

        FlyAiHotelItem expensive = hotel("高档酒店", "¥520", "春熙路1号", "高档型");
        FlyAiHotelItem affordable = hotel("实惠酒店", "¥260", "春熙路2号", "经济型");
        FlyAiHotelItem tooCheap = hotel("低价旅馆", "¥90", "春熙路3号", "经济型");
        when(flyAiService.searchHotels("成都", "春熙路", 600))
                .thenReturn(response(expensive, affordable, tooCheap));

        var result = travelHotelService.searchHotelsForPlan(
                7L, 10L, new HotelSearchCriteria(
                        HotelLocationType.LANDMARK,
                        "春熙路",
                        200,
                        600));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).hotelName()).isEqualTo("实惠酒店");
        assertThat(result.get(0).profileUsed()).isTrue();
        assertThat(result.get(0).recommendationReasons())
                .anyMatch(reason -> reason.contains("性价比偏好"));
        assertThat(result.get(0).recommendationReasons())
                .anyMatch(reason -> reason.contains("地标附近"));
        verify(flyAiService).searchHotels("成都", "春熙路", 600);
    }

    @Test
    void shouldRejectInvalidPriceRangeBeforeCallingFlyAi() {
        assertThatThrownBy(() -> travelHotelService.searchHotelsForPlan(
                7L, 10L, new HotelSearchCriteria(null, 600, 200)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("最低价不能高于最高价");

        verify(flyAiService, never()).searchHotels("成都", null, 200);
    }

    @Test
    void shouldExplainWhenUserHasNoAccommodationProfile() {
        TravelPlan plan = new TravelPlan();
        plan.setDestination("厦门");
        when(travelPlanService.getMyPlanById(7L, 11L)).thenReturn(plan);
        when(userProfileService.getProfile(7L)).thenReturn(null);
        when(flyAiService.searchHotels("厦门", null, null))
                .thenReturn(response(hotel("示例酒店", "¥300", "厦门市", "舒适型")));

        var result = travelHotelService.searchHotelsForPlan(
                7L, 11L, new HotelSearchCriteria(null, null, null));

        assertThat(result).singleElement().satisfies(hotel -> {
            assertThat(hotel.profileUsed()).isFalse();
            assertThat(hotel.recommendationReasons())
                    .anyMatch(reason -> reason.contains("尚未填写住宿偏好"));
        });
    }

    private FlyAiHotelItem hotel(String name, String price, String address, String star) {
        FlyAiHotelItem item = new FlyAiHotelItem();
        item.setName(name);
        item.setPrice(price);
        item.setAddress(address);
        item.setStar(star);
        item.setLatitude("30.657000");
        item.setLongitude("104.066000");
        item.setMainPic("https://example.test/hotel.jpg");
        item.setDetailUrl("https://example.test/hotel");
        return item;
    }

    private FlyAiHotelResponse response(FlyAiHotelItem... hotels) {
        FlyAiHotelData data = new FlyAiHotelData();
        data.setItemList(List.of(hotels));
        FlyAiHotelResponse response = new FlyAiHotelResponse();
        response.setData(data);
        return response;
    }
}
