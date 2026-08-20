package com.lanyu.xiaolanaitravel.travel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** 用户从候选方案中明确选择、准备加入正式行程的节点。 */
public record TravelDraftConfirmRequest(
        @NotEmpty(message = "请至少选择一个候选节点")
        List<@NotBlank(message = "候选节点标识不能为空") String> draftItemKeys) {
}
