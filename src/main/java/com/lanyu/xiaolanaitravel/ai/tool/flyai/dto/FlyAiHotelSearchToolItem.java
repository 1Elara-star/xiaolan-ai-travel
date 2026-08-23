package com.lanyu.xiaolanaitravel.ai.tool.flyai.dto;

/**
 * 返回给 AI 层的单个真实酒店候选。
 */
public record FlyAiHotelSearchToolItem(
        String name,
        String price,
        String address,
        String longitude,
        String latitude,
        String imageUrl,
        String detailUrl,
        String star,
        String brandName
) {
}
