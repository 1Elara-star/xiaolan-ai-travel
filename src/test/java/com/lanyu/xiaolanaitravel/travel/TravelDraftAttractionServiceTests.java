package com.lanyu.xiaolanaitravel.travel;

import com.lanyu.xiaolanaitravel.explore.dto.AttractionResponse;
import com.lanyu.xiaolanaitravel.favorite.dto.FavoriteAttractionResponse;
import com.lanyu.xiaolanaitravel.travel.dto.TravelPlanDraft;
import com.lanyu.xiaolanaitravel.travel.dto.TravelPlanDraftItem;
import com.lanyu.xiaolanaitravel.travel.service.TravelDraftAttractionService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TravelDraftAttractionServiceTests {

    private final TravelDraftAttractionService service =
            new TravelDraftAttractionService();

    @Test
    void enrichesCandidateCardFromOwnedFavoriteWithoutExternalApi() {
        TravelPlanDraftItem item = item("D1-I1", 88L, "鼓浪屿");
        TravelPlanDraft draft = draft(item);
        AttractionResponse attraction = new AttractionResponse(
                88L,
                "鼓浪屿",
                "厦门",
                "海上花园",
                "人文",
                "/images/gulangyu.jpg",
                "岛上保留了多元建筑与社区生活。",
                "适合慢慢散步，感受建筑和海岛日常。",
                List.of("人文", "散步"),
                "3小时",
                "清晨光线柔和",
                "节假日提前预约",
                "厦门市思明区鼓浪屿",
                new BigDecimal("118.067"),
                new BigDecimal("24.447"),
                "ATTRACTION",
                "全天开放",
                "船票需预约"
        );

        service.enrichFromFavorites(
                draft,
                List.of(new FavoriteAttractionResponse(
                        3L,
                        LocalDateTime.now(),
                        attraction
                ))
        );

        assertEquals("FAVORITE", item.getSourceType());
        assertEquals(88L, item.getAttractionId());
        assertEquals("/images/gulangyu.jpg", item.getImageUrl());
        assertEquals(attraction.story(), item.getStoryBackground());
        assertEquals(attraction.popularReason(), item.getFeatureDescription());
        assertEquals(new BigDecimal("118.067"), item.getLongitude());
        assertEquals(new BigDecimal("24.447"), item.getLatitude());
        assertEquals(180, item.getSuggestDuration());
    }

    @Test
    void clearsUnknownFavoriteIdInsteadOfTrustingAi() {
        TravelPlanDraftItem item = item("D1-I1", 999L, "未知景点");

        service.enrichFromFavorites(draft(item), List.of(
                new FavoriteAttractionResponse(
                        3L,
                        LocalDateTime.now(),
                        new AttractionResponse(
                                88L, "鼓浪屿", "厦门", null,
                                null, null, null, null, List.of(),
                                null, null, null, null, null, null,
                                "ATTRACTION", null, null
                        )
                )
        ));

        assertNull(item.getAttractionId());
        assertEquals("AI_RECOMMENDED", item.getSourceType());
    }

    private TravelPlanDraftItem item(
            String key,
            Long attractionId,
            String placeName) {
        TravelPlanDraftItem item = new TravelPlanDraftItem();
        item.setDraftItemKey(key);
        item.setAttractionId(attractionId);
        item.setSourceType("AI_RECOMMENDED");
        item.setItemType("ATTRACTION");
        item.setPlaceName(placeName);
        return item;
    }

    private TravelPlanDraft draft(TravelPlanDraftItem item) {
        TravelPlanDraft draft = new TravelPlanDraft();
        draft.setItems(new java.util.ArrayList<>(List.of(item)));
        return draft;
    }
}
