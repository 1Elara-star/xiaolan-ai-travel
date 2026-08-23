package com.lanyu.xiaolanaitravel.ai.tool.amap.dto;

/**
 * AmapPoiSearchTool 的输入参数。
 *
 * @param keyword 要查询的具体地点关键词，例如“鼓浪屿”“夫子庙”
 * @param city    地点所在城市，例如“厦门”“南京”
 * @param limit   高德返回的候选 POI 数量，后续 Tool 会限制在 1~5
 */
public record AmapPoiSearchToolRequest(
        String keyword,
        String city,
        Integer limit
) {
}