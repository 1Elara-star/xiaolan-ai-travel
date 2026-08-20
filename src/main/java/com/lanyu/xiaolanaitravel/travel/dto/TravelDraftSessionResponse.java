package com.lanyu.xiaolanaitravel.travel.dto;

import java.time.LocalDateTime;

/**
 * 返回给前端的候选行程会话。
 *
 * 不包含内部使用的 userId 和 planId。
 */
public record TravelDraftSessionResponse(
        String draftId,
        LocalDateTime expiresAt,
        TravelPlanDraft draft
) {
}
