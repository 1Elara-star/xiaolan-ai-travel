package com.lanyu.xiaolanaitravel.amap.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/** 高德 POI 搜索结果中随地点一起返回的图片。 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AmapPoiPhoto {
    private String title;
    private String url;
}
