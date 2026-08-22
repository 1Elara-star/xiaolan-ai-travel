package com.lanyu.xiaolanaitravel.travel;

import com.lanyu.xiaolanaitravel.explore.entity.Attraction;
import com.lanyu.xiaolanaitravel.explore.mapper.AttractionMapper;
import com.lanyu.xiaolanaitravel.explore.service.ExploreService;
import com.lanyu.xiaolanaitravel.favorite.entity.AttractionFavorite;
import com.lanyu.xiaolanaitravel.favorite.mapper.AttractionFavoriteMapper;
import com.lanyu.xiaolanaitravel.travel.entity.TravelPlan;
import com.lanyu.xiaolanaitravel.travel.entity.TravelPlanItem;
import com.lanyu.xiaolanaitravel.travel.mapper.TravelPlanItemMapper;
import com.lanyu.xiaolanaitravel.travel.service.AttractionRecommendationService;
import com.lanyu.xiaolanaitravel.travel.service.TravelDistanceService;
import com.lanyu.xiaolanaitravel.travel.service.TravelPlanService;
import com.lanyu.xiaolanaitravel.user.entity.UserProfile;
import com.lanyu.xiaolanaitravel.user.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttractionRecommendationServiceTests {

    @Mock private TravelPlanService travelPlanService;
    @Mock private UserProfileService userProfileService;
    @Mock private AttractionMapper attractionMapper;
    @Mock private AttractionFavoriteMapper favoriteMapper;
    @Mock private TravelPlanItemMapper planItemMapper;
    @Mock private ExploreService exploreService;

    private AttractionRecommendationService service;

    @BeforeEach
    void setUp() {
        service = new AttractionRecommendationService(
                travelPlanService, userProfileService, attractionMapper, favoriteMapper,
                planItemMapper, new TravelDistanceService(), exploreService);
        TravelPlan plan = new TravelPlan();
        plan.setId(9L);
        plan.setUserId(7L);
        plan.setDestination("厦门市");
        plan.setTripType("人文摄影");
        when(travelPlanService.getMyPlanById(7L, 9L)).thenReturn(plan);
        when(planItemMapper.selectList(any())).thenReturn(List.of());
        when(favoriteMapper.selectList(any())).thenReturn(List.of());
        when(exploreService.toResponse(any())).thenAnswer(invocation -> {
            Attraction item = invocation.getArgument(0);
            return new com.lanyu.xiaolanaitravel.explore.dto.AttractionResponse(
                    item.getId(), item.getName(), item.getCity(), item.getType(), item.getType(),
                    item.getImageUrl(), item.getStoryBackground(), item.getFeatureDescription(),
                    List.of(), "", "", item.getAvoidTags(), item.getAddress(),
                    item.getLongitude(), item.getLatitude(), item.getType(), null, null);
        });
    }

    @Test
    void shouldRankProfileAndTripMatchesFirst() {
        UserProfile profile = new UserProfile();
        profile.setInterestTags("历史,建筑,拍照");
        when(userProfileService.getProfile(7L)).thenReturn(profile);
        Attraction history = attraction(1L, "南普陀寺", "历史人文", "古寺,建筑", null);
        Attraction beach = attraction(2L, "海边公园", "自然风光", "海岸,散步", null);
        when(attractionMapper.selectList(any())).thenReturn(List.of(beach, history));

        var result = service.recommend(7L, 9L, null);

        assertThat(result).extracting(item -> item.attraction().name())
                .containsExactly("南普陀寺", "海边公园");
        assertThat(result.get(0).profileScore()).isGreaterThan(0);
        assertThat(result.get(0).recommendationReasons())
                .anyMatch(reason -> reason.contains("符合你的兴趣"));
    }

    @Test
    void shouldPenalizeDislikedExperience() {
        UserProfile profile = new UserProfile();
        profile.setDislikeTags("不喜欢人多和排队");
        when(userProfileService.getProfile(7L)).thenReturn(profile);
        Attraction crowded = attraction(1L, "热门古街", "历史人文", "历史", "节假日客流较大");
        Attraction quiet = attraction(2L, "安静小院", "历史人文", "历史,安静", "需提前预约");
        when(attractionMapper.selectList(any())).thenReturn(List.of(crowded, quiet));

        var result = service.recommend(7L, 9L, null);

        assertThat(result.get(0).attraction().name()).isEqualTo("安静小院");
        assertThat(result.stream().filter(item -> item.attraction().name().equals("热门古街"))
                .findFirst().orElseThrow().dislikePenalty()).isGreaterThan(0);
    }

    @Test
    void shouldLearnPreferenceFromFavorites() {
        when(userProfileService.getProfile(7L)).thenReturn(null);
        Attraction favoriteAttraction = attraction(20L, "杜甫草堂", "历史人文", "文学,园林", null);
        Attraction similar = attraction(1L, "文化名人故居", "历史人文", "文学,园林", null);
        Attraction different = attraction(2L, "游乐园", "主题乐园", "刺激,娱乐", null);
        AttractionFavorite favorite = new AttractionFavorite();
        favorite.setAttractionId(20L);
        when(favoriteMapper.selectList(any())).thenReturn(List.of(favorite));
        when(attractionMapper.selectByIds(any())).thenReturn(List.of(favoriteAttraction));
        when(attractionMapper.selectList(any())).thenReturn(List.of(different, similar));

        var result = service.recommend(7L, 9L, null);

        assertThat(result.get(0).attraction().name()).isEqualTo("文化名人故居");
        assertThat(result.get(0).favoriteScore()).isGreaterThan(0);
    }

    @Test
    void shouldUseExistingCoordinatesWithoutCallingMapApi() {
        when(userProfileService.getProfile(7L)).thenReturn(null);
        TravelPlanItem anchor = new TravelPlanItem();
        anchor.setPlaceName("当前节点");
        anchor.setLongitude(new BigDecimal("118.080000"));
        anchor.setLatitude(new BigDecimal("24.440000"));
        when(planItemMapper.selectList(any())).thenReturn(List.of(anchor));
        Attraction nearby = attraction(1L, "附近景点", "城市漫步", "建筑", null);
        nearby.setLongitude(new BigDecimal("118.081000"));
        nearby.setLatitude(new BigDecimal("24.441000"));
        Attraction faraway = attraction(2L, "远处景点", "城市漫步", "建筑", null);
        faraway.setLongitude(new BigDecimal("118.200000"));
        faraway.setLatitude(new BigDecimal("24.550000"));
        when(attractionMapper.selectList(any())).thenReturn(List.of(faraway, nearby));

        var result = service.recommend(7L, 9L, null);

        assertThat(result.get(0).attraction().name()).isEqualTo("附近景点");
        assertThat(result.get(0).nearestPlanDistanceMeters()).isBetween(1, 1_000);
        assertThat(result.get(0).geographyScore()).isEqualTo(15);
    }

    @Test
    void shouldExcludeAttractionsAlreadyInPlan() {
        when(userProfileService.getProfile(7L)).thenReturn(null);
        TravelPlanItem existing = new TravelPlanItem();
        existing.setAttractionId(1L);
        existing.setPlaceName("已经安排");
        when(planItemMapper.selectList(any())).thenReturn(List.of(existing));
        when(attractionMapper.selectList(any())).thenReturn(List.of(
                attraction(1L, "已经安排", "历史人文", "历史", null),
                attraction(2L, "新景点", "历史人文", "历史", null)));

        var result = service.recommend(7L, 9L, null);

        assertThat(result).singleElement()
                .satisfies(item -> assertThat(item.attraction().name()).isEqualTo("新景点"));
    }

    private Attraction attraction(Long id, String name, String type, String tags, String avoidTags) {
        Attraction attraction = new Attraction();
        attraction.setId(id);
        attraction.setName(name);
        attraction.setCity("厦门");
        attraction.setType(type);
        attraction.setSuitableTags(tags);
        attraction.setAvoidTags(avoidTags);
        attraction.setDescription(name + "介绍");
        return attraction;
    }
}
