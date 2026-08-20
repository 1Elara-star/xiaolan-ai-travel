package com.lanyu.xiaolanaitravel.travel.dto;

import jakarta.validation.constraints.Size;

/** 用户针对本次候选方案补充的自然语言要求。 */
public record TravelAiGenerateRequest(
        @Size(max = 500, message = "补充要求不能超过500字")
        String additionalRequirements) {
}
