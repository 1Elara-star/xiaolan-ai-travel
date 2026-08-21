package com.lanyu.xiaolanaitravel.travel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lanyu.xiaolanaitravel.amap.dto.AmapPoiItem;
import com.lanyu.xiaolanaitravel.amap.dto.AmapRouteResult;
import com.lanyu.xiaolanaitravel.amap.dto.AmapTravelMode;
import com.lanyu.xiaolanaitravel.amap.service.AmapPoiMatcher;
import com.lanyu.xiaolanaitravel.amap.service.AmapPoiSearchCache;
import com.lanyu.xiaolanaitravel.amap.service.AmapService;
import com.lanyu.xiaolanaitravel.travel.dto.TravelItemLocationResponse;
import com.lanyu.xiaolanaitravel.travel.dto.TravelItemRouteResponse;
import com.lanyu.xiaolanaitravel.travel.entity.TravelPlan;
import com.lanyu.xiaolanaitravel.travel.entity.TravelPlanItem;
import com.lanyu.xiaolanaitravel.travel.mapper.TravelPlanItemMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/** 将高德地点和路线数据回填到用户自己的旅行行程节点。 */
@Service
public class TravelMapService {

    private final TravelPlanService travelPlanService;
    private final TravelPlanItemMapper travelPlanItemMapper;
    private final AmapService amapService;
    private final AmapPoiSearchCache poiSearchCache;
    private final AmapPoiMatcher poiMatcher;

    public TravelMapService(
            TravelPlanService travelPlanService,
            TravelPlanItemMapper travelPlanItemMapper,
            AmapService amapService,
            AmapPoiSearchCache poiSearchCache,
            AmapPoiMatcher poiMatcher) {
        this.travelPlanService = travelPlanService;
        this.travelPlanItemMapper = travelPlanItemMapper;
        this.amapService = amapService;
        this.poiSearchCache = poiSearchCache;
        this.poiMatcher = poiMatcher;
    }

    /**
     * 使用一次 POI 请求匹配单个节点，并保存真实地址和经纬度。
     */
    public TravelItemLocationResponse resolveItemLocation(
            Long userId,
            Long planId,
            Long itemId) {
        return resolveItemLocation(userId, planId, itemId, false);
    }

    public TravelItemLocationResponse resolveItemLocation(
            Long userId,
            Long planId,
            Long itemId,
            boolean refresh) {
        TravelPlan plan = travelPlanService.getMyPlanById(userId, planId);
        TravelPlanItem item = getOwnedItem(planId, itemId);

        if (!refresh && item.getLongitude() != null && item.getLatitude() != null
                && item.getAddress() != null && !item.getAddress().isBlank()
                && item.getCityCode() != null && !item.getCityCode().isBlank()) {
            return new TravelItemLocationResponse(
                    item.getId(), null, item.getPlaceName(), item.getAddress(),
                    item.getLongitude(), item.getLatitude(), item.getCityCode(),
                    null,
                    "DATABASE_CACHE", null);
        }

        List<AmapPoiItem> candidates = poiSearchCache.getOrLoad(
                item.getPlaceName(),
                plan.getDestination(),
                3,
                refresh,
                () -> amapService.searchPois(item.getPlaceName(), plan.getDestination(), 3));
        AmapPoiItem selected = poiMatcher.findBest(
                        item.getPlaceName(), plan.getDestination(), candidates)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "高德没有找到当前目的地内的对应地点"));
        BigDecimal[] coordinate = parseCoordinate(selected.getLocation());

        item.setAddress(resolveAddress(selected));
        item.setLongitude(coordinate[0]);
        item.setLatitude(coordinate[1]);
        item.setCityCode(requireCityCode(selected));
        updateItem(item);

        return new TravelItemLocationResponse(
                item.getId(),
                selected.getId(),
                selected.getName(),
                item.getAddress(),
                item.getLongitude(),
                item.getLatitude(),
                item.getCityCode(),
                firstPhotoUrl(selected),
                "AMAP_POI_2.0",
                LocalDateTime.now());
    }

    /**
     * 使用一次路线请求，计算当前节点与前一个节点之间的距离和耗时。
     */
    public TravelItemRouteResponse calculateRouteFromPrevious(
            Long userId,
            Long planId,
            Long itemId,
            AmapTravelMode mode) {
        return calculateRouteFromPrevious(userId, planId, itemId, mode, false);
    }

    public TravelItemRouteResponse calculateRouteFromPrevious(
            Long userId,
            Long planId,
            Long itemId,
            AmapTravelMode mode,
            boolean refresh) {
        travelPlanService.getMyPlanById(userId, planId);
        TravelPlanItem current = getOwnedItem(planId, itemId);
        TravelPlanItem previous = findPreviousItem(current);

        AmapTravelMode selectedMode = mode == null ? AmapTravelMode.WALKING : mode;
        if (!refresh
                && selectedMode.name().equals(current.getTransportMode())
                && current.getDistanceFromPrev() != null
                && current.getTravelTimeFromPrev() != null) {
            return new TravelItemRouteResponse(
                    previous.getId(), current.getId(), selectedMode,
                    current.getDistanceFromPrev(), null,
                    current.getTravelTimeFromPrev(), "DATABASE_CACHE", null);
        }

        requireCoordinate(previous, "前一个行程节点还没有真实经纬度，请先匹配地点");
        requireCoordinate(current, "当前行程节点还没有真实经纬度，请先匹配地点");
        if (selectedMode == AmapTravelMode.TRANSIT) {
            requireCityCode(previous, "前一个行程节点缺少城市编码，请重新匹配地点");
            requireCityCode(current, "当前行程节点缺少城市编码，请重新匹配地点");
        }

        AmapRouteResult route = amapService.calculateRoute(
                previous.getLongitude(),
                previous.getLatitude(),
                current.getLongitude(),
                current.getLatitude(),
                previous.getCityCode(),
                current.getCityCode(),
                selectedMode);

        int durationMinutes = Math.max(1, (route.durationSeconds() + 59) / 60);
        current.setTransportMode(route.mode().name());
        current.setDistanceFromPrev(route.distanceMeters());
        current.setTravelTimeFromPrev(durationMinutes);
        updateItem(current);

        return new TravelItemRouteResponse(
                previous.getId(),
                current.getId(),
                route.mode(),
                route.distanceMeters(),
                route.durationSeconds(),
                durationMinutes,
                "AMAP_ROUTE_2.0",
                LocalDateTime.now());
    }

    private TravelPlanItem getOwnedItem(Long planId, Long itemId) {
        TravelPlanItem item = travelPlanItemMapper.selectOne(
                new LambdaQueryWrapper<TravelPlanItem>()
                        .eq(TravelPlanItem::getId, itemId)
                        .eq(TravelPlanItem::getPlanId, planId));
        if (item == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "行程节点不存在");
        }
        return item;
    }

    private TravelPlanItem findPreviousItem(TravelPlanItem current) {
        TravelPlanItem previous = travelPlanItemMapper.selectOne(
                new LambdaQueryWrapper<TravelPlanItem>()
                        .eq(TravelPlanItem::getPlanId, current.getPlanId())
                        .eq(TravelPlanItem::getDayNumber, current.getDayNumber())
                        .lt(TravelPlanItem::getItemOrder, current.getItemOrder())
                        .orderByDesc(TravelPlanItem::getItemOrder)
                        .orderByDesc(TravelPlanItem::getId)
                        .last("LIMIT 1"));
        if (previous == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前节点是当天第一站，没有可计算的上一站路线");
        }
        return previous;
    }

    private BigDecimal[] parseCoordinate(String location) {
        try {
            String[] parts = location.split(",", -1);
            if (parts.length != 2) {
                throw new NumberFormatException("coordinate parts");
            }
            BigDecimal longitude = new BigDecimal(parts[0]).setScale(6, RoundingMode.HALF_UP);
            BigDecimal latitude = new BigDecimal(parts[1]).setScale(6, RoundingMode.HALF_UP);
            if (longitude.compareTo(BigDecimal.valueOf(-180)) < 0
                    || longitude.compareTo(BigDecimal.valueOf(180)) > 0
                    || latitude.compareTo(BigDecimal.valueOf(-90)) < 0
                    || latitude.compareTo(BigDecimal.valueOf(90)) > 0) {
                throw new NumberFormatException("coordinate range");
            }
            return new BigDecimal[]{longitude, latitude};
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "高德地点经纬度格式不正确");
        }
    }

    private String resolveAddress(AmapPoiItem poi) {
        if (poi.getAddress() != null && !poi.getAddress().isBlank()) {
            return poi.getAddress().strip();
        }
        return joinAddress(poi.getPname(), poi.getCityname(), poi.getAdname());
    }

    private String requireCityCode(AmapPoiItem poi) {
        if (poi.getCitycode() == null || poi.getCitycode().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "高德地点结果缺少城市编码");
        }
        return poi.getCitycode().strip();
    }

    private String firstPhotoUrl(AmapPoiItem poi) {
        if (poi.getPhotos() == null) {
            return null;
        }
        return poi.getPhotos().stream()
                .filter(java.util.Objects::nonNull)
                .map(photo -> photo.getUrl())
                .filter(url -> url != null && !url.isBlank())
                .map(String::strip)
                .findFirst()
                .orElse(null);
    }

    private String joinAddress(String... parts) {
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (part != null && !part.isBlank() && !result.toString().contains(part.strip())) {
                result.append(part.strip());
            }
        }
        return result.isEmpty() ? null : result.toString();
    }

    private void requireCoordinate(TravelPlanItem item, String message) {
        if (item.getLongitude() == null || item.getLatitude() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private void requireCityCode(TravelPlanItem item, String message) {
        if (item.getCityCode() == null || item.getCityCode().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private void updateItem(TravelPlanItem item) {
        if (travelPlanItemMapper.updateById(item) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "行程节点不存在");
        }
    }
}
