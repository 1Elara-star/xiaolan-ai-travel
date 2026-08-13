package com.lanyu.xiaolanaitravel.memory.dto;

import java.time.LocalDateTime;

public record UserMemoryResponse(
        Long id,
        String memoryType,
        String memoryContent,
        boolean userConfirmed,
        LocalDateTime createTime,
        LocalDateTime updateTime) {
}
