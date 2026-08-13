package com.lanyu.xiaolanaitravel.favorite.dto;

import com.lanyu.xiaolanaitravel.explore.dto.AttractionResponse;

import java.time.LocalDateTime;

public record FavoriteAttractionResponse(
        Long favoriteId,
        LocalDateTime favoritedAt,
        AttractionResponse attraction) {
}
