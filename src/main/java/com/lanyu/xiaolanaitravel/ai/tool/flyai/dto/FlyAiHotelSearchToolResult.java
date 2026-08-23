package com.lanyu.xiaolanaitravel.ai.tool.flyai.dto;

import java.util.List;

/**
 * 飞猪酒店查询 Tool 的稳定输出，不直接暴露第三方响应结构。
 */
public record FlyAiHotelSearchToolResult(
        List<FlyAiHotelSearchToolItem> hotels,
        int count
) {

    public FlyAiHotelSearchToolResult {
        hotels = hotels == null ? List.of() : List.copyOf(hotels);
    }
}
