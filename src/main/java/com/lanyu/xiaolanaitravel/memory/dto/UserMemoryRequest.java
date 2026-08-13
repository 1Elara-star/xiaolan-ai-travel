package com.lanyu.xiaolanaitravel.memory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserMemoryRequest(
        @NotBlank(message = "记忆类型不能为空")
        @Pattern(regexp = "PREFERENCE|DISLIKE|EXPERIENCE|REMINDER",
                message = "记忆类型不支持")
        String memoryType,

        @NotBlank(message = "记忆内容不能为空")
        @Size(max = 500, message = "记忆内容不能超过500字")
        String memoryContent,

        Boolean userConfirmed) {
}
