package com.lanyu.xiaolanaitravel.travel.dto;

import java.time.LocalDateTime;

/**
 * 保存在服务器内存中的候选行程会话。
 */
public class TravelDraftSession {

    private final String draftId;
    private final Long userId;
    private final Long planId;
    private final TravelPlanDraft draft;
    private final LocalDateTime createdAt;
    private final LocalDateTime expiresAt;

    public TravelDraftSession(
            String draftId,
            Long userId,
            Long planId,
            TravelPlanDraft draft,
            LocalDateTime createdAt,
            LocalDateTime expiresAt) {
        this.draftId = draftId;
        this.userId = userId;
        this.planId = planId;
        this.draft = draft;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public String getDraftId() {
        return draftId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getPlanId() {
        return planId;
    }

    public TravelPlanDraft getDraft() {
        return draft;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
}
