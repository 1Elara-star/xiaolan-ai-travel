package com.lanyu.xiaolanaitravel.travel.dto;

import java.util.List;

/** 部分采用候选节点后的正式行程和剩余候选方案。 */
public record TravelDraftConfirmationResponse(
        List<TravelPlanItemResponse> planItems,
        TravelDraftSessionResponse draftSession) {
}
