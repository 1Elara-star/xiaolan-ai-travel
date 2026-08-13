package com.lanyu.xiaolanaitravel.favorite.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lanyu.xiaolanaitravel.explore.dto.AttractionResponse;
import com.lanyu.xiaolanaitravel.explore.entity.Attraction;
import com.lanyu.xiaolanaitravel.explore.mapper.AttractionMapper;
import com.lanyu.xiaolanaitravel.explore.service.ExploreService;
import com.lanyu.xiaolanaitravel.favorite.dto.FavoriteAttractionResponse;
import com.lanyu.xiaolanaitravel.favorite.dto.FavoriteStatusResponse;
import com.lanyu.xiaolanaitravel.favorite.entity.AttractionFavorite;
import com.lanyu.xiaolanaitravel.favorite.mapper.AttractionFavoriteMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AttractionFavoriteService {

    private final AttractionFavoriteMapper favoriteMapper;
    private final AttractionMapper attractionMapper;
    private final ExploreService exploreService;

    public AttractionFavoriteService(
            AttractionFavoriteMapper favoriteMapper,
            AttractionMapper attractionMapper,
            ExploreService exploreService) {
        this.favoriteMapper = favoriteMapper;
        this.attractionMapper = attractionMapper;
        this.exploreService = exploreService;
    }

    public List<FavoriteAttractionResponse> list(Long userId) {
        List<AttractionFavorite> favorites = favoriteMapper.selectList(
                new LambdaQueryWrapper<AttractionFavorite>()
                        .eq(AttractionFavorite::getUserId, userId)
                        .orderByDesc(AttractionFavorite::getCreateTime)
                        .orderByDesc(AttractionFavorite::getId));
        if (favorites.isEmpty()) {
            return List.of();
        }

        List<Long> attractionIds = favorites.stream()
                .map(AttractionFavorite::getAttractionId).distinct().toList();
        Map<Long, Attraction> attractionMap = attractionMapper.selectBatchIds(attractionIds).stream()
                .collect(Collectors.toMap(Attraction::getId, Function.identity()));

        return favorites.stream()
                .map(favorite -> {
                    Attraction attraction = attractionMap.get(favorite.getAttractionId());
                    AttractionResponse response = attraction == null
                            ? null : exploreService.toResponse(attraction);
                    return new FavoriteAttractionResponse(
                            favorite.getId(), favorite.getCreateTime(), response);
                })
                .filter(item -> item.attraction() != null)
                .toList();
    }

    /** Idempotent: repeated collection returns the same favorited state. */
    @Transactional
    public FavoriteStatusResponse add(Long userId, Long attractionId) {
        ensureAttractionExists(attractionId);
        if (find(userId, attractionId) != null) {
            return new FavoriteStatusResponse(attractionId, true);
        }
        AttractionFavorite favorite = new AttractionFavorite();
        favorite.setUserId(userId);
        favorite.setAttractionId(attractionId);
        try {
            favoriteMapper.insert(favorite);
        } catch (DuplicateKeyException ignored) {
            // The database unique constraint makes simultaneous repeated clicks idempotent.
        }
        return new FavoriteStatusResponse(attractionId, true);
    }

    /** Idempotent and user-scoped: it never deletes another user's favorite. */
    @Transactional
    public FavoriteStatusResponse remove(Long userId, Long attractionId) {
        favoriteMapper.delete(new LambdaQueryWrapper<AttractionFavorite>()
                .eq(AttractionFavorite::getUserId, userId)
                .eq(AttractionFavorite::getAttractionId, attractionId));
        return new FavoriteStatusResponse(attractionId, false);
    }

    public FavoriteStatusResponse status(Long userId, Long attractionId) {
        return new FavoriteStatusResponse(attractionId, find(userId, attractionId) != null);
    }

    private AttractionFavorite find(Long userId, Long attractionId) {
        return favoriteMapper.selectOne(new LambdaQueryWrapper<AttractionFavorite>()
                .eq(AttractionFavorite::getUserId, userId)
                .eq(AttractionFavorite::getAttractionId, attractionId)
                .last("LIMIT 1"));
    }

    private void ensureAttractionExists(Long attractionId) {
        if (attractionMapper.selectById(attractionId) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "景点不存在");
        }
    }
}
