package com.lanyu.xiaolanaitravel.ai.tool.amap.dto;

import java.math.BigDecimal;

/**
 * 高德公交路线 Tool 输入。
 *
 * @param departureDate    可选出发日期，格式 yyyy-MM-dd
 * @param departureTime    可选出发时间，格式 HH:mm
 * @param considerNightBus 是否考虑夜班公交
 */
public record AmapTransitRouteToolRequest(
        BigDecimal originLongitude,
        BigDecimal originLatitude,
        BigDecimal destinationLongitude,
        BigDecimal destinationLatitude,
        String originCityCode,
        String destinationCityCode,
        String departureDate,
        String departureTime,
        Boolean considerNightBus
) {
}
