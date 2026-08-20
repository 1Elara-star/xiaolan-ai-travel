package com.lanyu.xiaolanaitravel.user.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record AdminRoleUpdateRequest(
        @NotNull(message = "角色不能为空")
        @Pattern(regexp = "USER|ADMIN", message = "角色只支持 USER 或 ADMIN")
        String role) {
}
