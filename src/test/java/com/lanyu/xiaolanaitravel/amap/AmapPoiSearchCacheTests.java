package com.lanyu.xiaolanaitravel.amap;

import com.lanyu.xiaolanaitravel.amap.dto.AmapPoiItem;
import com.lanyu.xiaolanaitravel.amap.service.AmapPoiSearchCache;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AmapPoiSearchCacheTests {

    @Test
    void sameCityAndPlaceLoadsOnlyOnce() {
        AmapPoiSearchCache cache = new AmapPoiSearchCache();
        AtomicInteger loads = new AtomicInteger();

        cache.getOrLoad("中山公园", "厦门", 3,
                () -> result(loads, "厦门中山公园"));
        List<AmapPoiItem> second = cache.getOrLoad(" 中山公园 ", "厦门市", 3,
                () -> result(loads, "不应再次加载"));

        assertEquals(1, loads.get());
        assertEquals("厦门中山公园", second.get(0).getName());
    }

    @Test
    void samePlaceInDifferentCitiesUsesDifferentEntries() {
        AmapPoiSearchCache cache = new AmapPoiSearchCache();
        AtomicInteger loads = new AtomicInteger();

        cache.getOrLoad("中山公园", "厦门", 3,
                () -> result(loads, "厦门中山公园"));
        List<AmapPoiItem> shanghai = cache.getOrLoad("中山公园", "上海", 3,
                () -> result(loads, "上海中山公园"));

        assertEquals(2, loads.get());
        assertEquals("上海中山公园", shanghai.get(0).getName());
    }

    @Test
    void forceRefreshReplacesExistingEntry() {
        AmapPoiSearchCache cache = new AmapPoiSearchCache();
        AtomicInteger loads = new AtomicInteger();

        cache.getOrLoad("中山公园", "厦门", 3,
                () -> result(loads, "旧结果"));
        List<AmapPoiItem> refreshed = cache.getOrLoad(
                "中山公园", "厦门", 3, true,
                () -> result(loads, "新结果"));

        assertEquals(2, loads.get());
        assertEquals("新结果", refreshed.get(0).getName());
    }

    private List<AmapPoiItem> result(AtomicInteger loads, String name) {
        loads.incrementAndGet();
        AmapPoiItem poi = new AmapPoiItem();
        poi.setName(name);
        return List.of(poi);
    }
}
