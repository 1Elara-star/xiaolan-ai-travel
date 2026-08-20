package com.lanyu.xiaolanaitravel.travel.dto;

import java.util.List;

/**
 * 小兰系统统一的酒店候选返回格式。
 */
public record HotelCandidateResponse(
        String hotelName,
        String price,
        String address,
        String latitude,
        String longitude,
        String imageUrl,
        String detailUrl,
        String star,
        String brandName,
        String source,
        Integer priceValue,
        Integer tripMatchScore,
        Integer profileMatchScore,
        Integer overallMatchScore,
        boolean profileUsed,
        List<String> recommendationReasons
) {
}
