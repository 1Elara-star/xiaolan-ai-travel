package com.lanyu.xiaolanaitravel.user.dto;

import lombok.Data;

/**
 * 用户旅行画像保存/修改请求
 */
@Data
public class UserProfileRequest {

    private String mbti;

    private String travelPace;

    private String budgetPreference;

    private String transportPreference;

    private String interestTags;

    private String dislikeTags;

    private String specialNotes;

    private String companionPreference;

    private String foodPreference;

    private String mealStylePreference;

    private String restaurantPreference;

    private String accommodationPreference;
}