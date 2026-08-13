package com.lanyu.xiaolanaitravel.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户旅行画像保存/修改请求
 */
@Data
public class UserProfileRequest {

    @Pattern(regexp = "^$|^(E|I)(S|N)(T|F)(J|P)$", message = "MBTI格式不正确")
    private String mbti;

    @Size(max = 30, message = "旅行节奏不能超过30字")
    private String travelPace;

    @Size(max = 30, message = "预算偏好不能超过30字")
    private String budgetPreference;

    @Size(max = 30, message = "交通偏好不能超过30字")
    private String transportPreference;

    @Size(max = 500, message = "兴趣标签不能超过500字")
    private String interestTags;

    @Size(max = 500, message = "不喜欢标签不能超过500字")
    private String dislikeTags;

    @Size(max = 500, message = "特殊说明不能超过500字")
    private String specialNotes;

    @Size(max = 100, message = "同行偏好不能超过100字")
    private String companionPreference;

    @Size(max = 500, message = "饮食偏好不能超过500字")
    private String foodPreference;

    @Size(max = 500, message = "就餐偏好不能超过500字")
    private String mealStylePreference;

    @Size(max = 500, message = "餐厅偏好不能超过500字")
    private String restaurantPreference;

    @Size(max = 500, message = "住宿偏好不能超过500字")
    private String accommodationPreference;
}
