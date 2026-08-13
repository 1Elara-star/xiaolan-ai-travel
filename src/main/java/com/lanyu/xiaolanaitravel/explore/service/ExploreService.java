package com.lanyu.xiaolanaitravel.explore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lanyu.xiaolanaitravel.explore.dto.AttractionResponse;
import com.lanyu.xiaolanaitravel.explore.dto.CityExploreResponse;
import com.lanyu.xiaolanaitravel.explore.dto.CitySummaryResponse;
import com.lanyu.xiaolanaitravel.explore.entity.Attraction;
import com.lanyu.xiaolanaitravel.explore.mapper.AttractionMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ExploreService {

    private static final Map<String, CityPresentation> CITY_PRESENTATIONS = cityPresentations();

    private final AttractionMapper attractionMapper;

    public ExploreService(AttractionMapper attractionMapper) {
        this.attractionMapper = attractionMapper;
    }

    public List<CitySummaryResponse> listCities(String keyword) {
        List<Attraction> attractions = attractionMapper.selectList(
                new LambdaQueryWrapper<Attraction>()
                        .select(Attraction::getCity, Attraction::getId)
                        .isNotNull(Attraction::getCity)
                        .ne(Attraction::getCity, "")
                        .orderByAsc(Attraction::getCity));

        String normalized = trimToNull(keyword);
        return attractions.stream()
                .filter(item -> normalized == null || item.getCity().contains(normalized))
                .collect(Collectors.groupingBy(
                        Attraction::getCity, LinkedHashMap::new, Collectors.counting()))
                .entrySet().stream()
                .map(entry -> citySummary(entry.getKey(), entry.getValue()))
                .toList();
    }

    public CityExploreResponse getCity(String slugOrName) {
        String cityName = resolveCityName(slugOrName);
        List<Attraction> attractions = findAttractions(cityName, null, null);
        if (attractions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "城市不存在或暂无景点数据");
        }
        CityPresentation presentation = presentationFor(cityName);
        List<String> categories = attractions.stream()
                .map(Attraction::getType)
                .filter(Objects::nonNull)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
        return new CityExploreResponse(
                presentation.slug(), cityName, presentation.slogan(), presentation.description(),
                presentation.heroImage(), presentation.bestSeason(), presentation.recommendedDays(),
                categories, attractions.stream().map(this::toResponse).toList());
    }

    public List<AttractionResponse> listAttractions(String city, String keyword, String type) {
        return findAttractions(trimToNull(city), trimToNull(keyword), trimToNull(type)).stream()
                .map(this::toResponse)
                .toList();
    }

    public AttractionResponse getAttraction(Long id) {
        Attraction attraction = attractionMapper.selectById(id);
        if (attraction == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "景点不存在");
        }
        return toResponse(attraction);
    }

    private List<Attraction> findAttractions(String city, String keyword, String type) {
        LambdaQueryWrapper<Attraction> query = new LambdaQueryWrapper<>();
        query.eq(city != null, Attraction::getCity, city)
                .eq(type != null, Attraction::getType, type)
                .and(keyword != null, wrapper -> wrapper
                        .like(Attraction::getName, keyword)
                        .or().like(Attraction::getDescription, keyword)
                        .or().like(Attraction::getFeatureDescription, keyword)
                        .or().like(Attraction::getStoryBackground, keyword))
                .orderByAsc(Attraction::getCity)
                .orderByAsc(Attraction::getId);
        return attractionMapper.selectList(query);
    }

    private CitySummaryResponse citySummary(String cityName, long count) {
        CityPresentation presentation = presentationFor(cityName);
        return new CitySummaryResponse(
                presentation.slug(), cityName, presentation.slogan(), presentation.description(),
                presentation.heroImage(), presentation.bestSeason(), presentation.recommendedDays(), count);
    }

    public AttractionResponse toResponse(Attraction attraction) {
        String subtitle = firstNonBlank(attraction.getType(), attraction.getFeatureDescription());
        String reminder = joinNonBlank("；", attraction.getAvoidTags(), attraction.getOpenTime(),
                attraction.getTicketInfo());
        return new AttractionResponse(
                attraction.getId(), attraction.getName(), attraction.getCity(), subtitle,
                attraction.getType(),
                attraction.getImageUrl(), attraction.getStoryBackground(),
                firstNonBlank(attraction.getFeatureDescription(), attraction.getDescription()),
                splitTags(attraction.getSuitableTags()), formatDuration(attraction.getSuggestDuration()),
                "", reminder, attraction.getAddress(), attraction.getLongitude(), attraction.getLatitude(),
                attraction.getType(), attraction.getOpenTime(), attraction.getTicketInfo());
    }

    private String resolveCityName(String slugOrName) {
        String normalized = trimToNull(slugOrName);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "城市不能为空");
        }
        CityPresentation known = CITY_PRESENTATIONS.get(normalized.toLowerCase(Locale.ROOT));
        return known == null ? normalized : known.name();
    }

    private CityPresentation presentationFor(String cityName) {
        return CITY_PRESENTATIONS.values().stream()
                .filter(item -> item.name().equals(cityName))
                .findFirst()
                .orElseGet(() -> new CityPresentation(
                        cityName, cityName, "发现这座城市的独特风景", "从景点故事认识目的地。",
                        "", "", ""));
    }

    private static Map<String, CityPresentation> cityPresentations() {
        Map<String, CityPresentation> result = new LinkedHashMap<>();
        result.put("xiamen", new CityPresentation("xiamen", "厦门", "先认识这座海边城市，再决定想去哪里",
                "海风、老别墅与渔港日常交织在一起。", "/images/inspiration-coast.jpg", "10月—次年4月", "3—4天"));
        result.put("chengdu", new CityPresentation("chengdu", "成都", "先理解成都的松弛，再决定怎样慢慢逛",
                "历史、人情和美食藏在街巷与茶桌之间。", "/images/inspiration-city.jpg", "3—6月、9—11月", "4—5天"));
        result.put("suzhou", new CityPresentation("suzhou", "苏州", "先读懂园林与水巷，再决定怎样走进江南",
                "水、石、建筑与人的行走共同构成江南审美。", "/images/inspiration-nature.jpg", "3—5月、9—11月", "3—4天"));
        return Collections.unmodifiableMap(result);
    }

    private static List<String> splitTags(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split("[,，、;；]"))
                .map(String::trim).filter(item -> !item.isEmpty()).distinct().toList();
    }

    private static String formatDuration(Integer minutes) {
        if (minutes == null || minutes <= 0) return "";
        if (minutes < 60) return minutes + "分钟";
        int hours = minutes / 60;
        int remainder = minutes % 60;
        return remainder == 0 ? hours + "小时" : hours + "小时" + remainder + "分钟";
    }

    private static String firstNonBlank(String... values) {
        return Arrays.stream(values).filter(Objects::nonNull).filter(value -> !value.isBlank())
                .findFirst().orElse("");
    }

    private static String joinNonBlank(String delimiter, String... values) {
        return Arrays.stream(values).filter(Objects::nonNull).filter(value -> !value.isBlank())
                .collect(Collectors.joining(delimiter));
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private record CityPresentation(
            String slug, String name, String slogan, String description, String heroImage,
            String bestSeason, String recommendedDays) {
    }
}
