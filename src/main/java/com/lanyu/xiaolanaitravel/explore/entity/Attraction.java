package com.lanyu.xiaolanaitravel.explore.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Internal persistence model for the existing attraction table. */
@Data
@TableName("attraction")
public class Attraction {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String city;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String type;
    private String description;
    private String featureDescription;
    private String storyBackground;
    private String suitableTags;
    private String avoidTags;
    private Integer suggestDuration;
    private String openTime;
    private String ticketInfo;
    private String imageUrl;
    private LocalDateTime createTime;
}
