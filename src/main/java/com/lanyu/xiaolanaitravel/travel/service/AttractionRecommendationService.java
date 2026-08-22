package com.lanyu.xiaolanaitravel.travel.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lanyu.xiaolanaitravel.explore.entity.Attraction;
import com.lanyu.xiaolanaitravel.explore.mapper.AttractionMapper;
import com.lanyu.xiaolanaitravel.explore.service.ExploreService;
import com.lanyu.xiaolanaitravel.favorite.entity.AttractionFavorite;
import com.lanyu.xiaolanaitravel.favorite.mapper.AttractionFavoriteMapper;
import com.lanyu.xiaolanaitravel.travel.dto.AttractionRecommendationResponse;
import com.lanyu.xiaolanaitravel.travel.entity.TravelPlan;
import com.lanyu.xiaolanaitravel.travel.entity.TravelPlanItem;
import com.lanyu.xiaolanaitravel.travel.mapper.TravelPlanItemMapper;
import com.lanyu.xiaolanaitravel.user.entity.UserProfile;
import com.lanyu.xiaolanaitravel.user.service.UserProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * 基于内容、用户画像、收藏行为和地理距离的可解释景点推荐。
 *
 * <p>地理便利度只使用数据库中已有坐标计算直线距离，不调用高德接口，
 * 也不把直线距离冒充实际步行、骑行或驾车距离。</p>
 */
@Service
public class AttractionRecommendationService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 30;
    private static final List<Set<String>> SEMANTIC_GROUPS = List.of(
            Set.of("历史", "人文", "文化", "古建", "古寺", "三国", "文学", "博物馆"),
            Set.of("拍照", "摄影", "建筑", "日落", "海岸", "风景"),
            Set.of("美食", "小吃", "素斋", "茶馆", "市井"),
            Set.of("轻松", "慢游", "松弛", "安静", "慢生活", "休闲"),
            Set.of("自然", "海岛", "海岸", "园林", "山林"),
            Set.of("拥挤", "人多", "排队", "客流"),
            Set.of("走路", "步行", "爬坡", "台阶"),
            Set.of("晒", "怕晒", "暴晒")
    );

    private final TravelPlanService travelPlanService;
    private final UserProfileService userProfileService;
    private final AttractionMapper attractionMapper;
    private final AttractionFavoriteMapper favoriteMapper;
    private final TravelPlanItemMapper planItemMapper;
    private final TravelDistanceService distanceService;
    private final ExploreService exploreService;

    public AttractionRecommendationService(
            TravelPlanService travelPlanService,
            UserProfileService userProfileService,
            AttractionMapper attractionMapper,
            AttractionFavoriteMapper favoriteMapper,
            TravelPlanItemMapper planItemMapper,
            TravelDistanceService distanceService,
            ExploreService exploreService) {
        this.travelPlanService = travelPlanService;
        this.userProfileService = userProfileService;
        this.attractionMapper = attractionMapper;
        this.favoriteMapper = favoriteMapper;
        this.planItemMapper = planItemMapper;
        this.distanceService = distanceService;
        this.exploreService = exploreService;
    }

    public List<AttractionRecommendationResponse> recommend(
            Long userId,
            Long planId,
            Integer requestedLimit) {
        int limit = resolveLimit(requestedLimit);
        TravelPlan plan = travelPlanService.getMyPlanById(userId, planId);
        UserProfile profile = userProfileService.getProfile(userId);
        List<TravelPlanItem> planItems = loadPlanItems(planId);
        Set<Long> existingAttractionIds = new HashSet<>();
        Set<String> existingPlaceNames = new HashSet<>();
        List<Coordinate> planCoordinates = new ArrayList<>();
        for (TravelPlanItem item : planItems) {
            if (item == null) continue;
            if (item.getAttractionId() != null) existingAttractionIds.add(item.getAttractionId());
            String placeName = normalize(item.getPlaceName());
            if (placeName != null) existingPlaceNames.add(placeName.toLowerCase(Locale.ROOT));
            if (hasCoordinates(item.getLongitude(), item.getLatitude())) {
                planCoordinates.add(new Coordinate(item.getLongitude(), item.getLatitude()));
            }
        }

        List<Attraction> candidates = loadDestinationAttractions(plan.getDestination()).stream()
                .filter(item -> !existingAttractionIds.contains(item.getId()))
                .filter(item -> {
                    String name = normalize(item.getName());
                    return name == null || !existingPlaceNames.contains(name.toLowerCase(Locale.ROOT));
                })
                .toList();
        if (candidates.isEmpty()) return List.of();

        FavoriteContext favoriteContext = loadFavoriteContext(userId);
        String profileInterests = join(profile == null ? null : profile.getInterestTags(),
                profile == null ? null : profile.getTravelPace(),
                profile == null ? null : profile.getCompanionPreference(),
                profile == null ? null : profile.getFoodPreference());
        String constraints = join(profile == null ? null : profile.getDislikeTags(),
                plan.getSpecialRequirements());
        String tripNeeds = join(plan.getTripType(), plan.getTripPreferences(),
                plan.getCompanionType());
        boolean profileUsed = profileInterests != null
                || (profile != null && normalize(profile.getDislikeTags()) != null);

        return candidates.stream()
                .map(attraction -> score(attraction, profileInterests, constraints,
                        tripNeeds, favoriteContext, planCoordinates, profileUsed))
                .sorted(Comparator.comparingInt(AttractionRecommendationResponse::matchPercentage).reversed()
                        .thenComparing(AttractionRecommendationResponse::nearestPlanDistanceMeters,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(result -> result.attraction().id()))
                .limit(limit)
                .toList();
    }

    private AttractionRecommendationResponse score(
            Attraction attraction,
            String profileInterests,
            String constraints,
            String tripNeeds,
            FavoriteContext favoriteContext,
            List<Coordinate> planCoordinates,
            boolean profileUsed) {
        List<String> reasons = new ArrayList<>();
        List<String> candidateTerms = attractionTerms(attraction);
        String candidateText = attractionText(attraction);

        List<String> profileMatches = matchedTerms(profileInterests, candidateTerms, candidateText);
        int profileScore = Math.min(35, profileMatches.size() * 10);
        if (!profileMatches.isEmpty()) {
            reasons.add("符合你的兴趣：“" + String.join("、", profileMatches.stream().limit(3).toList()) + "”");
        }

        List<String> tripMatches = matchedTerms(tripNeeds, candidateTerms, candidateText);
        int tripScore = Math.min(25, tripMatches.size() * 8);
        if (!tripMatches.isEmpty()) {
            reasons.add("符合本次旅行需求：“" + String.join("、", tripMatches.stream().limit(3).toList()) + "”");
        }

        boolean isFavorite = favoriteContext.ids().contains(attraction.getId());
        int favoriteScore;
        if (isFavorite) {
            favoriteScore = 20;
            reasons.add("这是你已经收藏的景点");
        } else {
            List<String> affinityMatches = matchedTerms(
                    favoriteContext.preferenceText(), candidateTerms, candidateText);
            favoriteScore = Math.min(15, affinityMatches.size() * 5);
            if (favoriteScore > 0) {
                reasons.add("与你收藏过的景点类型相近");
            }
        }

        int dislikePenalty = calculateConstraintPenalty(constraints, attraction.getAvoidTags());
        if (dislikePenalty > 0) {
            reasons.add("可能与你的反感项或本次特别要求冲突，已降低推荐分");
        }

        Integer nearestDistance = nearestDistance(attraction, planCoordinates);
        int geographyScore = geographyScore(nearestDistance);
        if (nearestDistance != null) {
            reasons.add("距现有行程最近约" + formatDistance(nearestDistance) + "（直线距离）");
        }

        if (!profileUsed) {
            reasons.add("尚未填写兴趣画像，当前主要按本次行程和地理位置排序");
        }
        if (reasons.isEmpty()) {
            reasons.add("来自本次目的地的可选景点");
        }

        int matchPercentage = clamp(20 + profileScore + tripScore
                + favoriteScore + geographyScore - dislikePenalty);
        return new AttractionRecommendationResponse(
                exploreService.toResponse(attraction), matchPercentage, profileScore, tripScore,
                favoriteScore, geographyScore, dislikePenalty, nearestDistance, isFavorite,
                profileUsed, reasons.stream().distinct().limit(5).toList());
    }

    private List<Attraction> loadDestinationAttractions(String destination) {
        String city = normalizeCity(destination);
        if (city == null) return List.of();
        Set<String> variants = new LinkedHashSet<>();
        variants.add(city);
        variants.add(city + "市");
        return attractionMapper.selectList(new QueryWrapper<Attraction>()
                .in("city", variants)
                .orderByAsc("id"));
    }

    private List<TravelPlanItem> loadPlanItems(Long planId) {
        return planItemMapper.selectList(new QueryWrapper<TravelPlanItem>()
                .eq("plan_id", planId)
                .orderByAsc("day_number")
                .orderByAsc("item_order"));
    }

    private FavoriteContext loadFavoriteContext(Long userId) {
        List<AttractionFavorite> favorites = favoriteMapper.selectList(
                new QueryWrapper<AttractionFavorite>().eq("user_id", userId));
        Set<Long> ids = favorites.stream().map(AttractionFavorite::getAttractionId)
                .filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        if (ids.isEmpty()) return new FavoriteContext(Set.of(), null);
        List<Attraction> attractions = attractionMapper.selectByIds(ids);
        String preferenceText = attractions == null ? null : attractions.stream()
                .filter(Objects::nonNull)
                .map(this::attractionPreferenceText)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.joining(" "));
        return new FavoriteContext(ids, normalize(preferenceText));
    }

    private Integer nearestDistance(Attraction attraction, List<Coordinate> planCoordinates) {
        if (!hasCoordinates(attraction.getLongitude(), attraction.getLatitude())
                || planCoordinates.isEmpty()) return null;
        return planCoordinates.stream()
                .map(point -> distanceService.calculateStraightLineDistanceMeters(
                        point.longitude(), point.latitude(),
                        attraction.getLongitude(), attraction.getLatitude()))
                .min(Integer::compareTo)
                .orElse(null);
    }

    private int geographyScore(Integer distance) {
        if (distance == null) return 0;
        if (distance <= 1_000) return 15;
        if (distance <= 3_000) return 12;
        if (distance <= 8_000) return 7;
        if (distance <= 15_000) return 3;
        return 0;
    }

    private int calculateConstraintPenalty(String constraints, String avoidTags) {
        if (constraints == null || normalize(avoidTags) == null) return 0;
        int matches = matchedTerms(constraints, splitTerms(avoidTags), avoidTags).size();
        return Math.min(40, matches * 20);
    }

    private List<String> attractionTerms(Attraction attraction) {
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        terms.addAll(splitTerms(attraction.getType()));
        terms.addAll(splitTerms(attraction.getSuitableTags()));
        terms.addAll(splitTerms(attraction.getName()));
        return List.copyOf(terms);
    }

    private String attractionText(Attraction attraction) {
        return join(attraction.getName(), attraction.getType(), attraction.getSuitableTags(),
                attraction.getDescription(), attraction.getFeatureDescription(),
                attraction.getStoryBackground(), attraction.getAvoidTags());
    }

    private String attractionPreferenceText(Attraction attraction) {
        return join(attraction.getName(), attraction.getType(), attraction.getSuitableTags(),
                attraction.getDescription(), attraction.getFeatureDescription(),
                attraction.getStoryBackground());
    }

    private List<String> matchedTerms(String preferenceText, Collection<String> terms, String candidateText) {
        String preference = normalize(preferenceText);
        String candidate = normalize(candidateText);
        if (preference == null || candidate == null) return List.of();
        LinkedHashSet<String> matches = new LinkedHashSet<>();
        for (String term : terms) {
            String normalizedTerm = normalize(term);
            if (normalizedTerm != null && normalizedTerm.length() >= 2
                    && (preference.contains(normalizedTerm)
                    || semanticMatch(preference, normalizedTerm))) {
                matches.add(normalizedTerm);
            }
        }
        for (String preferenceTerm : splitTerms(preference)) {
            if (preferenceTerm.length() >= 2
                    && (candidate.contains(preferenceTerm)
                    || semanticMatch(candidate, preferenceTerm))) {
                matches.add(preferenceTerm);
            }
        }
        return List.copyOf(matches);
    }

    private boolean semanticMatch(String text, String term) {
        for (Set<String> group : SEMANTIC_GROUPS) {
            boolean termInGroup = group.stream().anyMatch(term::contains);
            if (termInGroup && group.stream().anyMatch(text::contains)) return true;
        }
        return false;
    }

    private List<String> splitTerms(String value) {
        String normalized = normalize(value);
        if (normalized == null) return List.of();
        return Arrays.stream(normalized.split("[,，、;；/|\\s]+"))
                .map(String::strip)
                .filter(item -> item.length() >= 2)
                .distinct()
                .toList();
    }

    private int resolveLimit(Integer requestedLimit) {
        if (requestedLimit == null) return DEFAULT_LIMIT;
        if (requestedLimit < 1 || requestedLimit > MAX_LIMIT) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "推荐数量必须在1到30之间");
        }
        return requestedLimit;
    }

    private String normalizeCity(String value) {
        String city = normalize(value);
        if (city != null && city.endsWith("市") && city.length() > 1) {
            return city.substring(0, city.length() - 1);
        }
        return city;
    }

    private String join(String... values) {
        String result = Arrays.stream(values)
                .map(this::normalize)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.joining(" "));
        return normalize(result);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip().toLowerCase(Locale.ROOT);
    }

    private boolean hasCoordinates(BigDecimal longitude, BigDecimal latitude) {
        return longitude != null && latitude != null;
    }

    private String formatDistance(int meters) {
        if (meters < 1_000) return meters + "米";
        return String.format(Locale.ROOT, "%.1f公里", meters / 1000D);
    }

    private int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private record Coordinate(BigDecimal longitude, BigDecimal latitude) {
    }

    private record FavoriteContext(Set<Long> ids, String preferenceText) {
    }
}
