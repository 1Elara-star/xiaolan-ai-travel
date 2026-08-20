package com.lanyu.xiaolanaitravel.travel.service;

import com.lanyu.xiaolanaitravel.travel.dto.TravelDraftSession;
import com.lanyu.xiaolanaitravel.travel.dto.TravelDraftConfirmationResponse;
import com.lanyu.xiaolanaitravel.travel.dto.TravelDraftSessionResponse;
import com.lanyu.xiaolanaitravel.travel.dto.TravelPlanDraftItem;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 将当前用户确认的候选行程转换成正式行程节点。
 */
@Service
public class TravelDraftConfirmationService {

    private final TravelDraftSessionService sessionService;
    private final TravelPlanItemService planItemService;

    public TravelDraftConfirmationService(
            TravelDraftSessionService sessionService,
            TravelPlanItemService planItemService) {
        this.sessionService = sessionService;
        this.planItemService = planItemService;
    }

    public TravelDraftConfirmationResponse confirm(
            Long userId,
            String draftId,
            List<String> selectedDraftItemKeys) {

        TravelDraftSession session =
                sessionService.getMySession(
                        userId,
                        draftId
                );

        Set<String> selectedKeys = normalizeKeys(
                selectedDraftItemKeys
        );
        List<TravelPlanDraftItem> selectedItems =
                session.getDraft().getItems().stream()
                        .filter(item -> selectedKeys.contains(
                                item.getDraftItemKey()
                        ))
                        .toList();

        if (selectedItems.size() != selectedKeys.size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "选择的候选节点不存在或已经采用"
            );
        }

        var planItems =
                planItemService.addFromDraft(
                        userId,
                        session.getPlanId(),
                        selectedItems
                );

        session.getDraft().getItems().removeIf(
                item -> selectedKeys.contains(
                        item.getDraftItemKey()
                )
        );

        TravelDraftSessionResponse remainingSession = null;
        if (session.getDraft().getItems().isEmpty()) {
            sessionService.removeSession(userId, draftId);
        } else {
            remainingSession = new TravelDraftSessionResponse(
                    session.getDraftId(),
                    session.getExpiresAt(),
                    session.getDraft()
            );
        }

        return new TravelDraftConfirmationResponse(
                planItems,
                remainingSession
        );
    }

    private Set<String> normalizeKeys(
            List<String> selectedDraftItemKeys) {

        if (selectedDraftItemKeys == null
                || selectedDraftItemKeys.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "请至少选择一个候选节点"
            );
        }

        Set<String> keys = new LinkedHashSet<>();
        for (String key : selectedDraftItemKeys) {
            if (key == null || key.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "候选节点标识不能为空"
                );
            }
            keys.add(key.strip());
        }
        return keys;
    }
}
