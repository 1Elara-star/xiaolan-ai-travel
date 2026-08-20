package com.lanyu.xiaolanaitravel.travel.service;

import com.lanyu.xiaolanaitravel.amap.dto.AmapPoiItem;
import com.lanyu.xiaolanaitravel.amap.service.AmapService;
import com.lanyu.xiaolanaitravel.travel.dto.TravelPlanDraft;
import com.lanyu.xiaolanaitravel.travel.dto.TravelPlanDraftItem;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 候选旅行方案地图补全服务。
 *
 * 负责在 TravelPlanDraft 尚未写入数据库之前，
 * 使用高德真实 POI 数据补全候选节点。
 *
 * 当前阶段只负责：
 * - POI 匹配
 * - 地址
 * - 经纬度
 * - 城市编码
 * - 同一天有效 POI 之间的直线距离预览
 *
 * straightLineDistanceFromPrev 只是地理直线距离，
 * 不是实际步行、公交、驾车或骑行路线距离。
 *
 * 暂时不负责：
 * - 路线计算
 * - 交通方式选择
 * - 时间冲突检查
 * - 数据库持久化
 */
@Service
public class TravelDraftMapService {

    /**
     * 第一版允许自动查询 POI 的节点类型。
     *
     * 当前只补全具体景点和活动。
     * FOOD、HOTEL、REST 和 OTHER 由各自业务流程处理，
     * 避免把泛化餐厅、待推荐酒店、休息或返程等节点发送给高德。
     */
    private static final Set<String> LOCATION_ITEM_TYPES =
            Set.of(
                    "ATTRACTION",
                    "EVENT"
            );

    /**
     * 明显不是具体 POI 的通用地点名称。
     *
     * 这些名称没有必要调用高德，
     * 可以减少无意义的 API 调用。
     */
    private static final Set<String> GENERIC_PLACE_NAMES =
            Set.of(
                    "待推荐酒店",
                    "住宿休息",
                    "休息",
                    "早餐",
                    "午餐",
                    "晚餐",
                    "用餐",
                    "当地美食",
                    "自由活动"
            );

    private final AmapService amapService;
    private final TravelDistanceService travelDistanceService;

    public TravelDraftMapService(
            AmapService amapService,
            TravelDistanceService travelDistanceService) {

        this.amapService = amapService;
        this.travelDistanceService = travelDistanceService;
    }

    /**
     * 为整份候选行程补全 POI 信息。
     *
     * 该方法直接修改传入的 TravelPlanDraft，
     * 同时返回同一个对象，
     * 方便后续 Workflow 继续链式处理。
     */
    public TravelPlanDraft enrichLocations(
            TravelPlanDraft draft) {

        if (draft == null) {
            throw new IllegalArgumentException(
                    "候选旅行方案不能为空"
            );
        }

        String destination =
                requireText(
                        draft.getDestination(),
                        "候选旅行方案缺少目的地"
                );

        List<TravelPlanDraftItem> items =
                draft.getItems();

        if (items == null || items.isEmpty()) {
            return draft;
        }

        /*
         * 本次规划过程中的临时缓存。
         *
         * key 示例：
         *
         * 南京|夫子庙
         *
         * value：
         * 高德匹配结果。
         *
         * Optional.empty() 也会缓存，
         * 这样同一个查不到的地点不会反复请求高德。
         */
        Map<String, Optional<AmapPoiItem>> poiCache =
                new HashMap<>();

        for (TravelPlanDraftItem item : items) {

            if (!shouldResolveLocation(item)) {
                continue;
            }

            /*
             * 如果这个 DraftItem 已经拥有真实坐标，
             * 不再次查询高德。
             *
             * 后续 Repair 时如果保留了原节点的地图数据，
             * 这一判断可以防止重复消耗 API。
             */
            if (hasCoordinate(item)) {
                continue;
            }

            String cacheKey =
                    buildCacheKey(
                            destination,
                            item.getPlaceName()
                    );

            Optional<AmapPoiItem> matchedPoi =
                    poiCache.computeIfAbsent(
                            cacheKey,
                            ignored -> searchBestPoi(
                                    item.getPlaceName(),
                                    destination
                            )
                    );

            /*
             * 高德没有找到合适 POI 时，
             * 当前阶段不让整份行程直接失败。
             *
             * 对应字段继续保持 null。
             *
             * 后续 TravelPlanValidationService
             * 会把这种情况转换成：
             *
             * POI_NOT_FOUND
             *
             * 再决定是否交给 Repair Agent。
             */
            if (matchedPoi.isEmpty()) {
                continue;
            }

            fillLocation(
                    item,
                    matchedPoi.get()
            );
        }

        return draft;
    }

    /**
     * 根据 Draft 中已有的经纬度，
     * 计算同一天相邻有效地点之间的直线距离预览。
     *
     * 该方法不会调用高德或数据库，
     * 也不会修改实际路线相关字段。
     */
    public TravelPlanDraft enrichStraightLineDistances(
            TravelPlanDraft draft) {

        if (draft == null) {
            throw new IllegalArgumentException(
                    "候选旅行方案不能为空"
            );
        }

        List<TravelPlanDraftItem> items =
                draft.getItems();

        if (items == null || items.isEmpty()) {
            return draft;
        }

        for (TravelPlanDraftItem item : items) {
            if (item != null) {
                item.setStraightLineDistanceFromPrev(null);
            }
        }

        List<TravelPlanDraftItem> sortedItems =
                new ArrayList<>(items);

        Comparator<TravelPlanDraftItem> itemComparator =
                Comparator.comparing(
                                TravelPlanDraftItem::getDayNumber,
                                Comparator.nullsLast(Integer::compareTo)
                        )
                        .thenComparing(
                                TravelPlanDraftItem::getItemOrder,
                                Comparator.nullsLast(Integer::compareTo)
                        );

        sortedItems.sort(
                Comparator.nullsLast(itemComparator)
        );

        Integer currentDayNumber = null;
        boolean firstItem = true;
        TravelPlanDraftItem previousLocatedItem = null;

        for (TravelPlanDraftItem currentItem : sortedItems) {
            if (currentItem == null) {
                continue;
            }

            if (firstItem
                    || !Objects.equals(
                    currentDayNumber,
                    currentItem.getDayNumber())) {

                currentDayNumber =
                        currentItem.getDayNumber();

                previousLocatedItem = null;
                firstItem = false;
            }

            if (!hasCoordinate(currentItem)) {
                continue;
            }

            if (previousLocatedItem != null) {
                int distanceMeters =
                        travelDistanceService
                                .calculateStraightLineDistanceMeters(
                                        previousLocatedItem.getLongitude(),
                                        previousLocatedItem.getLatitude(),
                                        currentItem.getLongitude(),
                                        currentItem.getLatitude()
                                );

                currentItem.setStraightLineDistanceFromPrev(
                        distanceMeters
                );
            }

            previousLocatedItem = currentItem;
        }

        return draft;
    }

    /**
     * 判断一个候选节点是否值得自动查询高德 POI。
     */
    private boolean shouldResolveLocation(
            TravelPlanDraftItem item) {

        if (item == null) {
            return false;
        }

        String itemType =
                normalize(item.getItemType());

        String placeName =
                normalize(item.getPlaceName());

        if (itemType == null
                || placeName == null) {

            return false;
        }

        if (!LOCATION_ITEM_TYPES.contains(
                itemType)) {

            return false;
        }

        /*
         * REST / OTHER 已经通过类型过滤。
         *
         * FOOD / HOTEL 中还可能出现：
         * 早餐、午餐、待推荐酒店等占位词，
         * 所以再进行一层名称过滤。
         */
        return !GENERIC_PLACE_NAMES.contains(
                placeName
        );
    }

    /**
     * 调用一次高德搜索，
     * 从候选结果中挑选最合适的 POI。
     */
    private Optional<AmapPoiItem> searchBestPoi(
            String placeName,
            String destination) {

        List<AmapPoiItem> candidates =
                amapService.searchPois(
                        placeName,
                        destination,
                        3
                );

        if (candidates == null
                || candidates.isEmpty()) {

            return Optional.empty();
        }

        String normalizedPlaceName =
                normalizeName(placeName);

        /*
         * 第一优先级：
         * 名称完全一致。
         */
        Optional<AmapPoiItem> exactMatch =
                candidates.stream()
                        .filter(this::isUsablePoi)
                        .filter(candidate ->
                                normalizeName(
                                        candidate.getName()
                                ).equals(
                                        normalizedPlaceName
                                )
                        )
                        .findFirst();

        if (exactMatch.isPresent()) {
            return exactMatch;
        }

        /*
         * 第二优先级：
         * 名称存在包含关系。
         *
         * 例如：
         *
         * AI：夫子庙
         * 高德：南京夫子庙
         */
        Optional<AmapPoiItem> containsMatch =
                candidates.stream()
                        .filter(this::isUsablePoi)
                        .filter(candidate -> {

                            String candidateName =
                                    normalizeName(
                                            candidate.getName()
                                    );

                            return candidateName.contains(
                                    normalizedPlaceName
                            ) || normalizedPlaceName.contains(
                                    candidateName
                            );
                        })
                        .findFirst();

        if (containsMatch.isPresent()) {
            return containsMatch;
        }

        /*
         * 第三优先级：
         * 高德已经根据“地点 + 城市”进行了搜索，
         * 如果没有完全匹配，
         * 使用第一个拥有合法经纬度的候选结果。
         */
        return candidates.stream()
                .filter(this::isUsablePoi)
                .findFirst();
    }

    /**
     * 将高德 POI 信息补进候选节点。
     */
    private void fillLocation(
            TravelPlanDraftItem item,
            AmapPoiItem poi) {

        BigDecimal[] coordinate =
                parseCoordinate(
                        poi.getLocation()
                );

        item.setPoiId(
                normalize(poi.getId())
        );

        item.setMatchedPoiName(
                normalize(poi.getName())
        );

        item.setAddress(
                resolveAddress(poi)
        );

        item.setLongitude(
                coordinate[0]
        );

        item.setLatitude(
                coordinate[1]
        );

        /*
         * 城市编码缺失时暂时允许为 null。
         *
         * 因为普通步行、驾车、骑行路线
         * 只需要经纬度。
         *
         * 如果后续选择公交 TRANSIT，
         * 再由路线阶段判断 cityCode 是否足够。
         */
        item.setCityCode(
                normalize(poi.getCitycode())
        );

        if (item.getImageUrl() == null) {
            item.setImageUrl(firstPhotoUrl(poi));
        }
    }

    private String firstPhotoUrl(AmapPoiItem poi) {
        if (poi.getPhotos() == null) {
            return null;
        }
        return poi.getPhotos().stream()
                .filter(Objects::nonNull)
                .map(photo -> normalize(photo.getUrl()))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /**
     * 判断高德候选 POI 是否至少拥有合法经纬度。
     */
    private boolean isUsablePoi(
            AmapPoiItem poi) {

        if (poi == null) {
            return false;
        }

        try {
            parseCoordinate(
                    poi.getLocation()
            );

            return true;

        } catch (IllegalArgumentException exception) {

            return false;
        }
    }

    /**
     * 解析高德：
     *
     * 经度,纬度
     *
     * 例如：
     * 118.796877,32.020694
     */
    private BigDecimal[] parseCoordinate(
            String location) {

        if (location == null
                || location.isBlank()) {

            throw new IllegalArgumentException(
                    "POI缺少经纬度"
            );
        }

        try {
            String[] parts =
                    location.split(",", -1);

            if (parts.length != 2) {
                throw new NumberFormatException(
                        "coordinate parts"
                );
            }

            BigDecimal longitude =
                    new BigDecimal(
                            parts[0].strip()
                    ).setScale(
                            6,
                            RoundingMode.HALF_UP
                    );

            BigDecimal latitude =
                    new BigDecimal(
                            parts[1].strip()
                    ).setScale(
                            6,
                            RoundingMode.HALF_UP
                    );

            if (longitude.compareTo(
                    BigDecimal.valueOf(-180)) < 0
                    || longitude.compareTo(
                    BigDecimal.valueOf(180)) > 0
                    || latitude.compareTo(
                    BigDecimal.valueOf(-90)) < 0
                    || latitude.compareTo(
                    BigDecimal.valueOf(90)) > 0) {

                throw new NumberFormatException(
                        "coordinate range"
                );
            }

            return new BigDecimal[]{
                    longitude,
                    latitude
            };

        } catch (RuntimeException exception) {

            throw new IllegalArgumentException(
                    "POI经纬度格式不正确",
                    exception
            );
        }
    }

    /**
     * 优先使用高德提供的完整 address。
     *
     * address 为空时，
     * 再使用省、市、区拼接一个基本地址。
     */
    private String resolveAddress(
            AmapPoiItem poi) {

        String address =
                normalize(poi.getAddress());

        if (address != null) {
            return address;
        }

        return joinAddress(
                poi.getPname(),
                poi.getCityname(),
                poi.getAdname()
        );
    }

    /**
     * 判断 DraftItem 是否已经拥有坐标。
     */
    private boolean hasCoordinate(
            TravelPlanDraftItem item) {

        return item.getLongitude() != null
                && item.getLatitude() != null;
    }

    /**
     * 构造一次规划过程中的 POI 缓存 Key。
     */
    private String buildCacheKey(
            String destination,
            String placeName) {

        return normalizeName(destination)
                + "|"
                + normalizeName(placeName);
    }

    /**
     * 地点名称标准化。
     *
     * 用于比较和缓存，
     * 不改变真正展示给用户的地点名称。
     */
    private String normalizeName(
            String value) {

        String normalized =
                normalize(value);

        if (normalized == null) {
            return "";
        }

        return normalized
                .replaceAll("\\s+", "")
                .toLowerCase(Locale.ROOT);
    }

    /**
     * 地址字段拼接。
     */
    private String joinAddress(
            String... parts) {

        StringBuilder result =
                new StringBuilder();

        for (String part : parts) {

            String normalized =
                    normalize(part);

            if (normalized != null
                    && !result.toString()
                    .contains(normalized)) {

                result.append(normalized);
            }
        }

        return result.isEmpty()
                ? null
                : result.toString();
    }

    /**
     * 空字符串统一转成 null。
     */
    private String normalize(
            String value) {

        return value == null
                || value.isBlank()
                ? null
                : value.strip();
    }

    /**
     * 必填字符串检查。
     */
    private String requireText(
            String value,
            String message) {

        String normalized =
                normalize(value);

        if (normalized == null) {
            throw new IllegalArgumentException(
                    message
            );
        }

        return normalized;
    }
}
