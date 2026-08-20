package com.lanyu.xiaolanaitravel.travel;

import com.lanyu.xiaolanaitravel.ai.service.DeepSeekService;
import com.lanyu.xiaolanaitravel.travel.dto.HotelLocationType;
import com.lanyu.xiaolanaitravel.travel.dto.HotelSearchCriteria;
import com.lanyu.xiaolanaitravel.travel.entity.TravelPlan;
import com.lanyu.xiaolanaitravel.travel.service.HotelPreferenceParsingService;
import com.lanyu.xiaolanaitravel.travel.service.TravelPlanService;
import com.lanyu.xiaolanaitravel.user.entity.UserProfile;
import com.lanyu.xiaolanaitravel.user.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HotelPreferenceParsingServiceTests {

    @Mock
    private DeepSeekService deepSeekService;
    @Mock
    private TravelPlanService travelPlanService;
    @Mock
    private UserProfileService userProfileService;

    private HotelPreferenceParsingService service;

    @BeforeEach
    void setUp() {
        service = new HotelPreferenceParsingService(
                deepSeekService,
                travelPlanService,
                userProfileService);
    }

    @Test
    void parsesNaturalLanguageWithPlanAndProfileContext() {
        TravelPlan plan = new TravelPlan();
        plan.setDestination("澳门");
        plan.setBudget(BigDecimal.valueOf(3500));
        when(travelPlanService.getMyPlanById(7L, 10L)).thenReturn(plan);

        UserProfile profile = new UserProfile();
        profile.setBudgetPreference("重视性价比");
        profile.setAccommodationPreference("安静、交通方便");
        when(userProfileService.getProfile(7L)).thenReturn(profile);

        HotelSearchCriteria parsed = new HotelSearchCriteria(
                HotelLocationType.LANDMARK,
                "银河综艺馆",
                300,
                600);
        when(deepSeekService.generateStructuredResponse(
                anyString(),
                anyString(),
                eq(HotelSearchCriteria.class)))
                .thenReturn(parsed);

        HotelSearchCriteria result = service.parse(
                7L,
                10L,
                "想住演唱会场馆附近，每晚三百到六百");

        assertThat(result).isEqualTo(parsed);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(deepSeekService).generateStructuredResponse(
                anyString(),
                messageCaptor.capture(),
                eq(HotelSearchCriteria.class));
        assertThat(messageCaptor.getValue())
                .contains("澳门", "演唱会场馆", "重视性价比", "安静、交通方便");
    }

    @Test
    void rejectsInvalidPriceRangeReturnedByAi() {
        TravelPlan plan = new TravelPlan();
        plan.setDestination("厦门");
        when(travelPlanService.getMyPlanById(7L, 10L)).thenReturn(plan);
        when(userProfileService.getProfile(7L)).thenReturn(null);
        when(deepSeekService.generateStructuredResponse(
                anyString(),
                anyString(),
                eq(HotelSearchCriteria.class)))
                .thenReturn(new HotelSearchCriteria(
                        HotelLocationType.SCENIC_AREA,
                        "鼓浪屿",
                        800,
                        300));

        assertThatThrownBy(() -> service.parse(7L, 10L, "住鼓浪屿附近"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("没有正确整理住宿条件");
    }
}
