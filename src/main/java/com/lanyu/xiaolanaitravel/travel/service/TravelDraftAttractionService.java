package com.lanyu.xiaolanaitravel.travel.service;

import com.lanyu.xiaolanaitravel.explore.dto.AttractionResponse;
import com.lanyu.xiaolanaitravel.favorite.dto.FavoriteAttractionResponse;
import com.lanyu.xiaolanaitravel.travel.dto.TravelPlanDraft;
import com.lanyu.xiaolanaitravel.travel.dto.TravelPlanDraftItem;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 使用用户自己的收藏景点补充候选卡片数据。
 *
 * 这里只读取已经从数据库取得的收藏结果，不调用高德或其他外部接口。
 */
@Service
public class TravelDraftAttractionService {

    public TravelPlanDraft enrichFromFavorites(
            TravelPlanDraft draft,
            List<FavoriteAttractionResponse> favorites) {

        List<AttractionResponse> attractions = favorites == null
                ? List.of()
                : favorites.stream()
                .filter(favorite -> favorite != null
                        && favorite.attraction() != null)
                .map(FavoriteAttractionResponse::attraction)
                .toList();
        Set<Long> favoriteIds = attractions.stream()
                .map(AttractionResponse::id)
                .collect(java.util.stream.Collectors.toSet());

        return enrichFromCatalog(draft, attractions, favoriteIds);
    }

    /**
     * 使用本地景点库补充候选卡片，并单独标记用户收藏。
     */
    public TravelPlanDraft enrichFromCatalog(
            TravelPlanDraft draft,
            List<AttractionResponse> attractions,
            Set<Long> favoriteIds) {

        if (draft == null) {
            throw new IllegalArgumentException("候选旅行方案不能为空");
        }

        if (draft.getItems() == null
                || draft.getItems().isEmpty()
                || attractions == null
                || attractions.isEmpty()) {
            return draft;
        }

        Map<Long, AttractionResponse> favoritesById =
                new HashMap<>();
        Map<String, AttractionResponse> favoritesByName =
                new HashMap<>();

        for (AttractionResponse attraction : attractions) {
            if (attraction == null) {
                continue;
            }
            favoritesById.put(attraction.id(), attraction);
            favoritesByName.put(
                    normalizeName(attraction.name()),
                    attraction
            );
        }

        for (TravelPlanDraftItem item : draft.getItems()) {
            if (item == null
                    || !"ATTRACTION".equals(item.getItemType())) {
                continue;
            }

            AttractionResponse attraction =
                    favoritesById.get(item.getAttractionId());

            if (attraction == null) {
                attraction = favoritesByName.get(
                        normalizeName(item.getPlaceName())
                );
            }

            if (attraction == null) {
                // 模型返回的未知 ID 不能进入正式行程。
                item.setAttractionId(null);
                continue;
            }

            applyAttraction(
                    item,
                    attraction,
                    favoriteIds != null
                            && favoriteIds.contains(attraction.id())
            );
        }

        return draft;
    }

    private void applyAttraction(
            TravelPlanDraftItem item,
            AttractionResponse attraction,
            boolean favorite) {

        item.setAttractionId(attraction.id());
        item.setSourceType(
                favorite
                        ? "FAVORITE"
                        : "LOCAL_ATTRACTION"
        );
        item.setPlaceName(attraction.name());
        item.setImageUrl(attraction.image());
        item.setStoryBackground(attraction.story());
        item.setFeatureDescription(attraction.popularReason());
        item.setSuitableTags(
                attraction.tags() == null
                        ? null
                        : String.join("，", attraction.tags())
        );
        item.setSuggestDuration(
                parseDurationMinutes(
                        attraction.suggestedDuration()
                )
        );
        item.setOpenTime(attraction.openTime());
        item.setTicketInfo(attraction.ticketInfo());

        if (item.getAddress() == null) {
            item.setAddress(attraction.address());
        }
        if (item.getLongitude() == null) {
            item.setLongitude(attraction.longitude());
        }
        if (item.getLatitude() == null) {
            item.setLatitude(attraction.latitude());
        }
    }

    private String normalizeName(String value) {
        return value == null
                ? ""
                : value.strip()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\s·•（）()—-]", "");
    }

    private Integer parseDurationMinutes(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.strip();
        try {
            if (normalized.endsWith("分钟")) {
                return Integer.parseInt(
                        normalized.substring(
                                0,
                                normalized.length() - 2
                        )
                );
            }
            if (normalized.endsWith("小时")) {
                return Integer.parseInt(
                        normalized.substring(
                                0,
                                normalized.length() - 2
                        )
                ) * 60;
            }
        } catch (NumberFormatException ignored) {
            // 展示文案不是可靠数值时不强行转换。
        }
        return null;
    }
}
