package com.lanyu.xiaolanaitravel.travel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 用户用自然语言表达的住宿需求。 */
public record HotelPreferenceParseRequest(
        @NotBlank(message = "请先说说你的住宿需求")
        @Size(max = 500, message = "住宿需求不能超过500个字符")
        String preference
) {
}
