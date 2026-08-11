package com.lanyu.xiaolanaitravel.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户旅行画像
 */
@Data
@TableName("user_profile")
public class UserProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * MBTI
     */
    private String mbti;

    /**
     * 旅行节奏偏好
     */
    private String travelPace;

    /**
     * 预算偏好
     */
    private String budgetPreference;

    /**
     * 交通偏好
     */
    private String transportPreference;

    /**
     * 兴趣标签
     */
    private String interestTags;

    /**
     * 不喜欢的内容
     */
    private String dislikeTags;

    /**
     * 特殊说明
     */
    private String specialNotes;

    /**
     * 同行偏好
     */
    private String companionPreference;

    /**
     * 饮食偏好
     */
    private String foodPreference;

    /**
     * 就餐方式偏好
     */
    private String mealStylePreference;

    /**
     * 餐厅类型偏好
     */
    private String restaurantPreference;

    /**
     * 住宿偏好
     */
    private String accommodationPreference;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}