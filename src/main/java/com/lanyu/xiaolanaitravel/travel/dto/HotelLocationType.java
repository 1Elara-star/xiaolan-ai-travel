package com.lanyu.xiaolanaitravel.travel.dto;

/** 酒店住宿位置偏好类型。 */
public enum HotelLocationType {
    BUSINESS_AREA("商圈"),
    TRANSPORT_HUB("交通枢纽"),
    METRO_STATION("地铁站"),
    SCENIC_AREA("景区附近"),
    LANDMARK("地标附近"),
    ADMINISTRATIVE_AREA("行政区域"),
    CUSTOM("自定义地点");

    private final String label;

    HotelLocationType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
