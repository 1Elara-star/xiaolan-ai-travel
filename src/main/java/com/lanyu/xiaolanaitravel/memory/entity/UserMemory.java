package com.lanyu.xiaolanaitravel.memory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_memory")
public class UserMemory {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String memoryType;
    private String memoryContent;
    private Long sourceFeedbackId;
    private Boolean userConfirmed;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
