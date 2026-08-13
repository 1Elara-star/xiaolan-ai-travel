package com.lanyu.xiaolanaitravel.explore.dto;

import java.math.BigDecimal;
import java.util.List;

/** Public attraction contract. It intentionally does not expose the persistence entity. */
public record AttractionResponse(
        Long id,
        String name,
        String city,
        String subtitle,
        String category,
        String image,
        String story,
        String popularReason,
        List<String> tags,
        String suggestedDuration,
        String photoTip,
        String reminder,
        String address,
        BigDecimal longitude,
        BigDecimal latitude,
        String type,
        String openTime,
        String ticketInfo) {
}
