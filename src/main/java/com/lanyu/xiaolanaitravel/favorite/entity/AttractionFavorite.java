package com.lanyu.xiaolanaitravel.favorite.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("attraction_favorite")
public class AttractionFavorite {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long attractionId;
    private LocalDateTime createTime;
}
