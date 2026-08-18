package com.lanyu.xiaolanaitravel.travel.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 单个行程节点匹配到的高德地点信息。 */
public record TravelItemLocationResponse(
        Long itemId,
        String poiId,
        String poiName,
        String address,
        BigDecimal longitude,
        BigDecimal latitude,
        String cityCode,
        String source,
        LocalDateTime queriedAt) {
}
