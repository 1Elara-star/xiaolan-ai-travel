package com.lanyu.xiaolanaitravel.travel.controller;

import com.lanyu.xiaolanaitravel.travel.dto.TravelDraftConfirmRequest;
import com.lanyu.xiaolanaitravel.travel.dto.TravelDraftConfirmationResponse;
import com.lanyu.xiaolanaitravel.travel.service.TravelDraftConfirmationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 候选行程确认接口。
 */
@RestController
@RequestMapping("/travel/draft/{draftId}")
public class TravelDraftController {

    private final TravelDraftConfirmationService confirmationService;

    public TravelDraftController(
            TravelDraftConfirmationService confirmationService) {
        this.confirmationService = confirmationService;
    }

    /**
     * 将候选方案采用为当前旅行的正式行程节点。
     */
    @PostMapping("/confirm")
    public ResponseEntity<TravelDraftConfirmationResponse> confirm(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable String draftId,
            @Valid @RequestBody TravelDraftConfirmRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(confirmationService.confirm(
                        userId,
                        draftId,
                        request.draftItemKeys()
                ));
    }
}
