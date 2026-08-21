package com.lanyu.xiaolanaitravel.amap.service;

import com.lanyu.xiaolanaitravel.amap.dto.AmapPoiItem;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 高德 POI 文本搜索的进程级缓存。
 *
 * <p>同一城市、同一地点在不同候选 Draft 或正式节点定位中重复出现时，
 * 优先复用已经取得的搜索结果，避免重复消耗关键词搜索额度。</p>
 *
 * <p>缓存仅存在于当前后端进程中，重启后自动清空，不修改数据库。</p>
 */
@Component
public class AmapPoiSearchCache {

    private static final Duration SUCCESS_TTL = Duration.ofHours(24);
    private static final Duration EMPTY_TTL = Duration.ofMinutes(10);
    private static final int MAX_ENTRIES = 1_000;

    private final Map<CacheKey, CacheEntry> cache = new ConcurrentHashMap<>();
    private final Clock clock;

    public AmapPoiSearchCache() {
        this(Clock.systemUTC());
    }

    AmapPoiSearchCache(Clock clock) {
        this.clock = clock;
    }

    /**
     * 读取缓存；未命中或已过期时才执行 loader。
     * 外部调用异常不会被缓存，方便后续正常重试。
     */
    public List<AmapPoiItem> getOrLoad(
            String keyword,
            String region,
            int limit,
            Supplier<List<AmapPoiItem>> loader) {

        return getOrLoad(keyword, region, limit, false, loader);
    }

    /** 强制刷新时忽略旧缓存，并用本次搜索结果替换缓存。 */
    public List<AmapPoiItem> getOrLoad(
            String keyword,
            String region,
            int limit,
            boolean forceRefresh,
            Supplier<List<AmapPoiItem>> loader) {

        CacheKey key = new CacheKey(
                normalizeRegion(region),
                normalize(keyword),
                limit
        );
        Instant now = clock.instant();

        CacheEntry existing = cache.get(key);
        if (!forceRefresh
                && existing != null
                && existing.expiresAt().isAfter(now)) {
            return existing.items();
        }

        removeExpired(now);
        if (cache.size() >= MAX_ENTRIES) {
            cache.clear();
        }

        CacheEntry loaded = cache.compute(key, (ignored, current) -> {
            Instant loadTime = clock.instant();
            if (!forceRefresh
                    && current != null
                    && current.expiresAt().isAfter(loadTime)) {
                return current;
            }

            List<AmapPoiItem> source = loader.get();
            List<AmapPoiItem> items = source == null
                    ? List.of()
                    : source.stream().filter(java.util.Objects::nonNull).toList();
            Duration ttl = items.isEmpty() ? EMPTY_TTL : SUCCESS_TTL;
            return new CacheEntry(items, loadTime.plus(ttl));
        });

        return loaded.items();
    }

    private void removeExpired(Instant now) {
        cache.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.strip()
                .replaceAll("\\s+", "")
                .toLowerCase(Locale.ROOT);
    }

    private String normalizeRegion(String value) {
        return normalize(value)
                .replaceFirst("(特别行政区|自治州|自治区|地区|市|盟)$", "");
    }

    private record CacheKey(String region, String keyword, int limit) {
    }

    private record CacheEntry(List<AmapPoiItem> items, Instant expiresAt) {
    }
}
