package com.lanyu.xiaolanaitravel.travel.controller;

import com.lanyu.xiaolanaitravel.travel.dto.TravelDraftSessionResponse;
import com.lanyu.xiaolanaitravel.travel.service.TravelDraftEnrichmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 候选行程地图数据补全接口。
 */
@RestController
@RequestMapping("/travel/draft/{draftId}/map")
public class TravelDraftMapController {

    private final TravelDraftEnrichmentService enrichmentService;

    public TravelDraftMapController(
            TravelDraftEnrichmentService enrichmentService) {
        this.enrichmentService = enrichmentService;
    }

    /**
     * 使用地图服务补全一个已经生成的候选行程。
     */
    @PostMapping("/enrich")
    public ResponseEntity<TravelDraftSessionResponse> enrichMap(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable String draftId) {

        return ResponseEntity.ok(
                enrichmentService.enrichMap(
                        userId,
                        draftId
                )
        );
    }
}
