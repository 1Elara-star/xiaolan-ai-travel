package com.lanyu.xiaolanaitravel.ai.tool.amap;

import com.lanyu.xiaolanaitravel.ai.tool.amap.dto.AmapPoiSearchToolRequest;
import com.lanyu.xiaolanaitravel.ai.tool.amap.dto.AmapPoiSearchToolResult;
import com.lanyu.xiaolanaitravel.amap.dto.AmapPoiItem;
import com.lanyu.xiaolanaitravel.amap.service.AmapPoiMatcher;
import com.lanyu.xiaolanaitravel.amap.service.AmapPoiSearchCache;
import com.lanyu.xiaolanaitravel.amap.service.AmapService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * AI 地点查询 Tool。
 *
 * 负责：
 * 1. 接收 AI 层地点查询需求
 * 2. 调用高德 POI 搜索能力
 * 3. 选择可信地点
 * 4. 返回稳定结果
 *
 * 不负责：
 * - 用户旅行业务
 * - 行程生成
 * - 数据库存储
 */
@Component
public class AmapPoiSearchTool {

    private static final int DEFAULT_LIMIT = 3;
    private static final int MAX_LIMIT = 5;

    private final AmapService amapService;
    private final AmapPoiSearchCache poiSearchCache;
    private final AmapPoiMatcher poiMatcher;


    public AmapPoiSearchTool(
            AmapService amapService,
            AmapPoiSearchCache poiSearchCache,
            AmapPoiMatcher poiMatcher) {

        this.amapService = amapService;
        this.poiSearchCache = poiSearchCache;
        this.poiMatcher = poiMatcher;
    }


    /**
     * 执行地点搜索。
     */
    public AmapPoiSearchToolResult execute(
            AmapPoiSearchToolRequest request) {

        validateRequest(request);

        String keyword = request.keyword().strip();
        String city = request.city().strip();
        int limit = normalizeLimit(request.limit());


        List<AmapPoiItem> candidates =
                poiSearchCache.getOrLoad(
                        keyword,
                        city,
                        limit,
                        () -> amapService.searchPois(
                                keyword,
                                city,
                                limit
                        )
                );


        Optional<AmapPoiItem> matched =
                poiMatcher.findBest(
                        keyword,
                        city,
                        candidates
                );


        AmapPoiItem poi = matched.orElseThrow(
                () -> new IllegalArgumentException(
                        "没有找到匹配的地点"
                )
        );


        return convert(poi);
    }


    private AmapPoiSearchToolResult convert(
            AmapPoiItem poi) {

        BigDecimal longitude = null;
        BigDecimal latitude = null;


        if (poi.getLocation() != null) {
            String[] parts = poi.getLocation().split(",", -1);
            if (parts.length != 2) {
                throw new IllegalArgumentException("匹配地点的坐标格式不正确");
            }
            try {
                longitude = new BigDecimal(parts[0].strip());
                latitude = new BigDecimal(parts[1].strip());
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("匹配地点的坐标格式不正确", exception);
            }
        }


        String imageUrl = null;

        if (poi.getPhotos() != null
                && !poi.getPhotos().isEmpty()
                && poi.getPhotos().get(0) != null) {

            imageUrl =
                    poi.getPhotos()
                            .get(0)
                            .getUrl();
        }


        return new AmapPoiSearchToolResult(
                poi.getId(),
                poi.getName(),
                poi.getAddress(),
                longitude,
                latitude,
                poi.getCitycode(),
                imageUrl
        );
    }


    private void validateRequest(
            AmapPoiSearchToolRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "地点查询参数不能为空"
            );
        }

        if (request.keyword() == null
                || request.keyword().isBlank()) {

            throw new IllegalArgumentException(
                    "地点关键词不能为空"
            );
        }


        if (request.city() == null
                || request.city().isBlank()) {

            throw new IllegalArgumentException(
                    "城市不能为空"
            );
        }
    }

    private int normalizeLimit(Integer requestedLimit) {
        if (requestedLimit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(requestedLimit, MAX_LIMIT));
    }
}
