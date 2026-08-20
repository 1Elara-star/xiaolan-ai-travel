package com.lanyu.xiaolanaitravel.user.dto;

import java.time.LocalDateTime;

public record AdminUserResponse(
        Long id,
        String username,
        String nickname,
        String email,
        String phone,
        String role,
        LocalDateTime createTime,
        LocalDateTime updateTime) {
}
