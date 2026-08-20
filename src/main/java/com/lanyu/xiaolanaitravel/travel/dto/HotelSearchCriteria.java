package com.lanyu.xiaolanaitravel.travel.dto;

/** 当前旅行的结构化酒店查询条件。 */
public record HotelSearchCriteria(
        HotelLocationType locationType,
        String locationKeyword,
        Integer minPrice,
        Integer maxPrice
) {

    /** 兼容已有的“商圈 + 价格”调用。 */
    public HotelSearchCriteria(String businessArea, Integer minPrice, Integer maxPrice) {
        this(businessArea == null || businessArea.isBlank()
                        ? null
                        : HotelLocationType.BUSINESS_AREA,
                businessArea,
                minPrice,
                maxPrice);
    }
}
