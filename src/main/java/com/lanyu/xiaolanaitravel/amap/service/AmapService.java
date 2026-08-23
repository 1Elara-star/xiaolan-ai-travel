package com.lanyu.xiaolanaitravel.amap.service;

import com.lanyu.xiaolanaitravel.amap.dto.AmapPoiItem;
import com.lanyu.xiaolanaitravel.amap.dto.AmapPoiSearchResponse;
import com.lanyu.xiaolanaitravel.amap.dto.AmapRouteApiResponse;
import com.lanyu.xiaolanaitravel.amap.dto.AmapRouteResult;
import com.lanyu.xiaolanaitravel.amap.dto.AmapTransitLineResult;
import com.lanyu.xiaolanaitravel.amap.dto.AmapTransitRouteResult;
import com.lanyu.xiaolanaitravel.amap.dto.AmapTravelMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * 高德 Web 服务客户端。
 *
 * <p>只负责第三方请求、响应校验和基础数据转换，不负责旅行计划业务。</p>
 */
@Service
public class AmapService {

    private final RestClient restClient;
    private final String apiKey;

    @Autowired
    public AmapService(
            @Value("${amap.api-key}") String apiKey,
            @Value("${amap.base-url}") String baseUrl) {
        this(createRestClient(baseUrl), apiKey);
    }

    AmapService(RestClient restClient, String apiKey) {
        this.restClient = restClient;
        this.apiKey = apiKey;
    }

    /**
     * 在指定城市内搜索地点。调用方应尽量传入准确地点名称，减少无效请求。
     */
    public List<AmapPoiItem> searchPois(String keyword, String region, int limit) {
        String normalizedKeyword = requireText(keyword, "地点关键词不能为空");
        String normalizedRegion = requireText(region, "搜索城市不能为空");
        int pageSize = Math.max(1, Math.min(limit, 5));

        try {
            AmapPoiSearchResponse response = restClient.get()
                    .uri(builder -> builder
                            .path("/v5/place/text")
                            .queryParam("key", apiKey)
                            .queryParam("keywords", normalizedKeyword)
                            .queryParam("region", normalizedRegion)
                            .queryParam("city_limit", true)
                            .queryParam("show_fields", "photos")
                            .queryParam("page_size", pageSize)
                            .queryParam("page_num", 1)
                            .queryParam("output", "json")
                            .build())
                    .retrieve()
                    .body(AmapPoiSearchResponse.class);

            validateResponse(response == null ? null : response.getStatus(),
                    response == null ? null : response.getInfocode(),
                    response == null ? null : response.getInfo());

            if (response.getPois() == null) {
                return List.of();
            }
            return response.getPois();
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new ResponseStatusException(BAD_GATEWAY, "高德地点服务暂时不可用");
        }
    }

    /**
     * 查询两点之间的一条路线。只请求 cost 字段，避免下载不需要的导航详情。
     */
    public AmapRouteResult calculateRoute(
            BigDecimal originLongitude,
            BigDecimal originLatitude,
            BigDecimal destinationLongitude,
            BigDecimal destinationLatitude,
            String originCityCode,
            String destinationCityCode,
            AmapTravelMode mode) {
        String origin = coordinate(originLongitude, originLatitude);
        String destination = coordinate(destinationLongitude, destinationLatitude);
        AmapTravelMode selectedMode = mode == null ? AmapTravelMode.WALKING : mode;
        String normalizedOriginCityCode = selectedMode == AmapTravelMode.TRANSIT
                ? requireText(originCityCode, "公交路线缺少起点城市编码") : null;
        String normalizedDestinationCityCode = selectedMode == AmapTravelMode.TRANSIT
                ? requireText(destinationCityCode, "公交路线缺少终点城市编码") : null;

        try {
            AmapRouteApiResponse response = restClient.get()
                    .uri(builder -> {
                        builder.path(selectedMode.getPath())
                                .queryParam("key", apiKey)
                                .queryParam("origin", origin)
                                .queryParam("destination", destination)
                                .queryParam("show_fields", "cost")
                                .queryParam("output", "json");
                        if (selectedMode == AmapTravelMode.DRIVING) {
                            builder.queryParam("strategy", 32);
                        } else if (selectedMode == AmapTravelMode.TRANSIT) {
                            builder.queryParam("city1", normalizedOriginCityCode)
                                    .queryParam("city2", normalizedDestinationCityCode)
                                    .queryParam("strategy", 0)
                                    .queryParam("AlternativeRoute", 1)
                                    .queryParam("nightflag", 1);
                        }
                        return builder.build();
                    })
                    .retrieve()
                    .body(AmapRouteApiResponse.class);

            validateResponse(response == null ? null : response.getStatus(),
                    response == null ? null : response.getInfocode(),
                    response == null ? null : response.getInfo());

            if (response.getRoute() == null) {
                throw new ResponseStatusException(NOT_FOUND, "高德没有找到可用路线");
            }

            if (selectedMode == AmapTravelMode.TRANSIT) {
                return parseTransitRoute(response, selectedMode);
            }

            if (response.getRoute().getPaths() == null
                    || response.getRoute().getPaths().isEmpty()) {
                throw new ResponseStatusException(NOT_FOUND, "高德没有找到可用路线");
            }

            AmapRouteApiResponse.Path firstPath = response.getRoute().getPaths().get(0);
            if (firstPath.getCost() == null) {
                throw new ResponseStatusException(BAD_GATEWAY, "高德路线结果缺少耗时信息");
            }
            return new AmapRouteResult(
                    selectedMode,
                    parseNonNegativeInt(firstPath.getDistance(), "路线距离"),
                    parseNonNegativeInt(firstPath.getCost().getDuration(), "路线耗时"));
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new ResponseStatusException(BAD_GATEWAY, "高德路线服务暂时不可用");
        }
    }

    /**
     * 查询包含公交/地铁线路和首末班时间的详细公交路线。
     *
     * <p>该方法只返回高德提供的事实数据，不判断用户是否一定能赶上末班车。</p>
     */
    public AmapTransitRouteResult calculateTransitRoute(
            BigDecimal originLongitude,
            BigDecimal originLatitude,
            BigDecimal destinationLongitude,
            BigDecimal destinationLatitude,
            String originCityCode,
            String destinationCityCode,
            LocalDate travelDate,
            LocalTime departureTime,
            boolean considerNightBus) {
        String origin = coordinate(originLongitude, originLatitude);
        String destination = coordinate(destinationLongitude, destinationLatitude);
        String normalizedOriginCityCode = requireText(
                originCityCode, "公交路线缺少起点城市编码");
        String normalizedDestinationCityCode = requireText(
                destinationCityCode, "公交路线缺少终点城市编码");

        try {
            AmapRouteApiResponse response = restClient.get()
                    .uri(builder -> {
                        builder.path(AmapTravelMode.TRANSIT.getPath())
                                .queryParam("key", apiKey)
                                .queryParam("origin", origin)
                                .queryParam("destination", destination)
                                .queryParam("city1", normalizedOriginCityCode)
                                .queryParam("city2", normalizedDestinationCityCode)
                                .queryParam("strategy", 0)
                                .queryParam("AlternativeRoute", 1)
                                .queryParam("nightflag", considerNightBus ? 1 : 0)
                                .queryParam("show_fields", "cost")
                                .queryParam("output", "json");
                        if (travelDate != null) {
                            builder.queryParam("date", travelDate);
                        }
                        if (departureTime != null) {
                            builder.queryParam("time", departureTime.format(
                                    DateTimeFormatter.ofPattern("H-mm")));
                        }
                        return builder.build();
                    })
                    .retrieve()
                    .body(AmapRouteApiResponse.class);

            validateResponse(response == null ? null : response.getStatus(),
                    response == null ? null : response.getInfocode(),
                    response == null ? null : response.getInfo());
            return parseTransitRouteDetails(response);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new ResponseStatusException(BAD_GATEWAY, "高德公交路线服务暂时不可用");
        }
    }

    private AmapRouteResult parseTransitRoute(
            AmapRouteApiResponse response,
            AmapTravelMode mode) {
        if (response.getRoute().getTransits() == null
                || response.getRoute().getTransits().isEmpty()) {
            throw new ResponseStatusException(NOT_FOUND, "高德没有找到可用公交路线");
        }
        AmapRouteApiResponse.Transit firstTransit = response.getRoute().getTransits().get(0);
        if (firstTransit.getCost() == null) {
            throw new ResponseStatusException(BAD_GATEWAY, "高德公交结果缺少耗时信息");
        }
        return new AmapRouteResult(
                mode,
                parseNonNegativeInt(firstTransit.getDistance(), "公交路线距离"),
                parseNonNegativeInt(firstTransit.getCost().getDuration(), "公交路线耗时"));
    }

    AmapTransitRouteResult parseTransitRouteDetails(AmapRouteApiResponse response) {
        if (response == null || response.getRoute() == null
                || response.getRoute().getTransits() == null
                || response.getRoute().getTransits().isEmpty()) {
            throw new ResponseStatusException(NOT_FOUND, "高德没有找到可用公交路线");
        }

        AmapRouteApiResponse.Transit transit = response.getRoute().getTransits().get(0);
        if (transit.getCost() == null) {
            throw new ResponseStatusException(BAD_GATEWAY, "高德公交结果缺少耗时信息");
        }

        List<AmapTransitLineResult> lines = new ArrayList<>();
        if (transit.getSegments() != null) {
            for (AmapRouteApiResponse.TransitSegment segment : transit.getSegments()) {
                if (segment == null || segment.getBus() == null
                        || segment.getBus().getBuslines() == null) {
                    continue;
                }
                for (AmapRouteApiResponse.BusLine line : segment.getBus().getBuslines()) {
                    if (line != null) {
                        lines.add(toTransitLineResult(line));
                    }
                }
            }
        }

        return new AmapTransitRouteResult(
                parseNonNegativeInt(transit.getDistance(), "公交路线距离"),
                parseNonNegativeInt(transit.getCost().getDuration(), "公交路线耗时"),
                "1".equals(transit.getNightflag()),
                lines
        );
    }

    private AmapTransitLineResult toTransitLineResult(AmapRouteApiResponse.BusLine line) {
        return new AmapTransitLineResult(
                normalizeOptional(line.getName()),
                normalizeOptional(line.getType()),
                stopName(line.getDepartureStop()),
                stopName(line.getArrivalStop()),
                parseOptionalNonNegativeInt(line.getDistance()),
                parseOptionalNonNegativeInt(line.getDuration()),
                normalizeTransitTime(line.getStartTime()),
                normalizeTransitTime(line.getEndTime()),
                normalizeTransitTime(line.getStationStartTime()),
                normalizeTransitTime(line.getStationEndTime())
        );
    }

    private String stopName(AmapRouteApiResponse.TransitStop stop) {
        return stop == null ? null : normalizeOptional(stop.getName());
    }

    private Integer parseOptionalNonNegativeInt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return parseNonNegativeInt(value.strip(), "公交分段数值");
    }

    private String normalizeTransitTime(String value) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            return null;
        }
        try {
            LocalTime time = normalized.matches("\\d{4}")
                    ? LocalTime.parse(normalized, DateTimeFormatter.ofPattern("HHmm"))
                    : LocalTime.parse(normalized, DateTimeFormatter.ofPattern("H:mm"));
            return time.format(DateTimeFormatter.ofPattern("HH:mm"));
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static RestClient createRestClient(String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    private void validateResponse(String status, String infocode, String info) {
        if ("1".equals(status) && "10000".equals(infocode)) {
            return;
        }
        String message = switch (infocode == null ? "" : infocode) {
            case "10001" -> "高德API Key无效或已过期";
            case "10002" -> "当前高德Key没有该服务权限";
            case "10003", "10004" -> "高德API调用额度或频率已受限";
            case "10005" -> "高德API的IP白名单校验失败";
            default -> "高德地图服务请求失败"
                    + (infocode == null || infocode.isBlank() ? "" : "（错误码" + infocode + "）");
        };
        throw new ResponseStatusException(BAD_GATEWAY, message);
    }

    private String coordinate(BigDecimal longitude, BigDecimal latitude) {
        if (longitude == null || latitude == null) {
            throw new IllegalArgumentException("路线起终点必须包含经纬度");
        }
        if (longitude.compareTo(BigDecimal.valueOf(-180)) < 0
                || longitude.compareTo(BigDecimal.valueOf(180)) > 0
                || latitude.compareTo(BigDecimal.valueOf(-90)) < 0
                || latitude.compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new IllegalArgumentException("路线起终点经纬度超出有效范围");
        }
        return longitude.setScale(6, RoundingMode.HALF_UP).toPlainString()
                + ","
                + latitude.setScale(6, RoundingMode.HALF_UP).toPlainString();
    }

    private int parseNonNegativeInt(String value, String fieldName) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 0) {
                throw new NumberFormatException("negative");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(BAD_GATEWAY, "高德返回的" + fieldName + "格式不正确");
        }
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.strip();
    }
}
