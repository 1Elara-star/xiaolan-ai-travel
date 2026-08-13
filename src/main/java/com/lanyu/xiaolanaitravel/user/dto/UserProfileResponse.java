package com.lanyu.xiaolanaitravel.user.dto;

public record UserProfileResponse(
        String mbti,
        String travelPace,
        String budgetPreference,
        String transportPreference,
        String interestTags,
        String dislikeTags,
        String specialNotes,
        String companionPreference,
        String foodPreference,
        String mealStylePreference,
        String restaurantPreference,
        String accommodationPreference) {
}
