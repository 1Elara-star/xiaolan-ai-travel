package com.lanyu.xiaolanaitravel.favorite.controller;

import com.lanyu.xiaolanaitravel.favorite.dto.FavoriteAttractionResponse;
import com.lanyu.xiaolanaitravel.favorite.dto.FavoriteStatusResponse;
import com.lanyu.xiaolanaitravel.favorite.service.AttractionFavoriteService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/favorites/attractions")
public class AttractionFavoriteController {

    private final AttractionFavoriteService favoriteService;

    public AttractionFavoriteController(AttractionFavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    public List<FavoriteAttractionResponse> list(
            @RequestAttribute("currentUserId") Long userId) {
        return favoriteService.list(userId);
    }

    @GetMapping("/{attractionId}/status")
    public FavoriteStatusResponse status(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable Long attractionId) {
        return favoriteService.status(userId, attractionId);
    }

    @PostMapping("/{attractionId}")
    public FavoriteStatusResponse add(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable Long attractionId) {
        return favoriteService.add(userId, attractionId);
    }

    @DeleteMapping("/{attractionId}")
    public FavoriteStatusResponse remove(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable Long attractionId) {
        return favoriteService.remove(userId, attractionId);
    }
}
