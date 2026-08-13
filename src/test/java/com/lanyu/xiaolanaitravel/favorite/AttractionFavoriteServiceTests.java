package com.lanyu.xiaolanaitravel.favorite;

import com.lanyu.xiaolanaitravel.explore.entity.Attraction;
import com.lanyu.xiaolanaitravel.explore.mapper.AttractionMapper;
import com.lanyu.xiaolanaitravel.explore.service.ExploreService;
import com.lanyu.xiaolanaitravel.favorite.dto.FavoriteStatusResponse;
import com.lanyu.xiaolanaitravel.favorite.entity.AttractionFavorite;
import com.lanyu.xiaolanaitravel.favorite.mapper.AttractionFavoriteMapper;
import com.lanyu.xiaolanaitravel.favorite.service.AttractionFavoriteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AttractionFavoriteServiceTests {

    private AttractionFavoriteMapper favoriteMapper;
    private AttractionMapper attractionMapper;
    private AttractionFavoriteService service;

    @BeforeEach
    void setUp() {
        favoriteMapper = mock(AttractionFavoriteMapper.class);
        attractionMapper = mock(AttractionMapper.class);
        service = new AttractionFavoriteService(
                favoriteMapper, attractionMapper, mock(ExploreService.class));
    }

    @Test
    void repeatedAddIsIdempotent() {
        when(attractionMapper.selectById(8L)).thenReturn(new Attraction());
        when(favoriteMapper.selectOne(any())).thenReturn(new AttractionFavorite());

        FavoriteStatusResponse response = service.add(3L, 8L);

        assertTrue(response.favorited());
        verify(favoriteMapper, never()).insert(any(AttractionFavorite.class));
    }

    @Test
    void repeatedRemoveIsIdempotent() {
        when(favoriteMapper.delete(any())).thenReturn(0);

        FavoriteStatusResponse response = service.remove(3L, 8L);

        assertFalse(response.favorited());
        verify(favoriteMapper).delete(any());
    }
}
