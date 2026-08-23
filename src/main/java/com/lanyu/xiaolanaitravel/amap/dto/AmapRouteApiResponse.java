package com.lanyu.xiaolanaitravel.amap.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
        private String nightflag;
        private List<TransitSegment> segments;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TransitSegment {
        private Bus bus;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Bus {
        private List<BusLine> buslines;
    }

    /** 公交或地铁线路信息，字段名称与高德响应保持对应。 */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BusLine {
        private String name;
        private String type;
        private String distance;
        private String duration;

        @JsonProperty("departure_stop")
        private TransitStop departureStop;

        @JsonProperty("arrival_stop")
        private TransitStop arrivalStop;

        @JsonProperty("start_time")
        private String startTime;

        @JsonProperty("end_time")
        private String endTime;

        @JsonProperty("station_start_time")
        private String stationStartTime;

        @JsonProperty("station_end_time")
        private String stationEndTime;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TransitStop {
        private String name;
        private String id;
        private String location;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Cost {
        /** 路线总耗时，单位：秒。 */
        private String duration;
    }
}
