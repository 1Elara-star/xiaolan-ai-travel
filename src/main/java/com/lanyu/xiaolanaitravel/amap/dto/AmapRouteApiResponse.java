package com.lanyu.xiaolanaitravel.amap.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/** 高德路径规划 2.0 原始响应，只映射项目当前需要的字段。 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AmapRouteApiResponse {

    private String status;
    private String info;
    private String infocode;
    private Route route;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Route {
        private List<Path> paths;
        private List<Transit> transits;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Path {
        /** 路线总距离，单位：米。 */
        private String distance;
        private Cost cost;
    }

    /** 公交路线方案。公交接口使用 transits，而不是 paths。 */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Transit {
        /** 路线总距离，单位：米。 */
        private String distance;
        private Cost cost;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Cost {
        /** 路线总耗时，单位：秒。 */
        private String duration;
    }
}
