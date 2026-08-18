package com.lanyu.xiaolanaitravel.amap.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 高德 POI 2.0 返回的单个地点数据。
 *
 * 这是第三方接口 DTO，
 * 字段名称尽量保持和高德返回值一致。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AmapPoiItem {

    /**
     * 高德 POI 唯一 ID。
     */
    private String id;

    /**
     * POI 名称。
     */
    private String name;

    /**
     * 详细地址。
     */
    private String address;

    /**
     * 经纬度字符串。
     *
     * 高德格式：
     * 经度,纬度
     *
     * 例如：
     * 117.293840,31.864372
     */
    private String location;

    /**
     * 所属省份。
     */
    private String pname;

    /**
     * 所属城市。
     */
    private String cityname;

    /**
     * 所属城市编码，公交路径规划的 city1/city2 参数使用该值。
     */
    private String citycode;

    /**
     * 所属区县。
     */
    private String adname;

    /**
     * POI 类型。
     */
    private String type;

    /**
     * POI 类型编码。
     */
    private String typecode;
}
