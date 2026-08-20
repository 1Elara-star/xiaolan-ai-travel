package com.lanyu.xiaolanaitravel.user.dto;

import java.time.LocalDateTime;

public record AdminAttractionResponse(
        Long id,
        String name,
        String city,
        String type,
        String address,
        LocalDateTime createTime) {
}
