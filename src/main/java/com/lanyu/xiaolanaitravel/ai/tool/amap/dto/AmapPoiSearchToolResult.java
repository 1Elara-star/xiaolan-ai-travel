package com.lanyu.xiaolanaitravel.ai.tool.amap.dto;

import java.math.BigDecimal;

/**
 * AmapPoiSearchTool 返回给 AI 层的地点结果。
 *
 * 这是 Tool 自己的稳定输出模型，
 * 不直接向 AI 暴露高德第三方 DTO AmapPoiItem。
 *
 * @param poiId     高德 POI 唯一 ID
 * @param name      匹配后的真实地点名称
 * @param address   地点地址
 * @param longitude 经度
 * @param latitude  纬度
 * @param cityCode  城市编码，后续公交路线查询可能使用
 * @param imageUrl  POI 搜索结果中携带的第一张图片
 */
public record AmapPoiSearchToolResult(
        String poiId,
        String name,
        String address,
        BigDecimal longitude,
        BigDecimal latitude,
        String cityCode,
        String imageUrl
) {
}