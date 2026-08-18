package com.lanyu.xiaolanaitravel.amap.dto;

/** 当前项目已接入的高德路线类型。 */
public enum AmapTravelMode {
    WALKING("/v5/direction/walking"),
    DRIVING("/v5/direction/driving"),
    BICYCLING("/v5/direction/bicycling"),
    TRANSIT("/v5/direction/transit/integrated");

    private final String path;

    AmapTravelMode(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
