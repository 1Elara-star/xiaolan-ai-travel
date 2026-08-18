package com.lanyu.xiaolanaitravel.amap.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/** 高德 POI 2.0 关键字搜索原始响应。 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AmapPoiSearchResponse {

    private String status;
    private String info;
    private String infocode;
    private String count;
    private List<AmapPoiItem> pois;
}
