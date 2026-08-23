package com.lanyu.xiaolanaitravel.ai.tool.flyai.dto;

/**
 * 飞猪酒店查询 Tool 输入。
 *
 * @param destination     旅行目的地，例如“成都”
 * @param locationKeyword 可选住宿位置，例如“春熙路”“双流机场”
 * @param maxPrice        可选每晚最高价格
 * @param limit           返回给 AI 的酒店数量，限制在 1~10
 */
public record FlyAiHotelSearchToolRequest(
        String destination,
        String locationKeyword,
        Integer maxPrice,
        Integer limit
) {
}
