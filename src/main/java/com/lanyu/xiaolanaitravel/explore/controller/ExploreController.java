package com.lanyu.xiaolanaitravel.explore.controller;

import com.lanyu.xiaolanaitravel.explore.dto.AttractionResponse;
import com.lanyu.xiaolanaitravel.explore.dto.CityExploreResponse;
import com.lanyu.xiaolanaitravel.explore.dto.CitySummaryResponse;
import com.lanyu.xiaolanaitravel.explore.service.ExploreService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Public read-only endpoints used by home search and city exploration pages. */
@RestController
@RequestMapping("/explore")
public class ExploreController {

    private final ExploreService exploreService;

    public ExploreController(ExploreService exploreService) {
        this.exploreService = exploreService;
    }

    @GetMapping("/cities")
    public List<CitySummaryResponse> listCities(
            @RequestParam(required = false) String keyword) {
        return exploreService.listCities(keyword);
    }

    @GetMapping("/cities/{slugOrName}")
    public CityExploreResponse getCity(@PathVariable String slugOrName) {
        return exploreService.getCity(slugOrName);
    }

    @GetMapping("/attractions")
    public List<AttractionResponse> listAttractions(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type) {
        return exploreService.listAttractions(city, keyword, type);
    }

    @GetMapping("/attractions/{id}")
    public AttractionResponse getAttraction(@PathVariable Long id) {
        return exploreService.getAttraction(id);
    }
}
