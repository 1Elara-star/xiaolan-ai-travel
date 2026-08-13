package com.lanyu.xiaolanaitravel.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.dao.DuplicateKeyException;
import com.lanyu.xiaolanaitravel.user.dto.UserProfileRequest;
import com.lanyu.xiaolanaitravel.user.dto.UserProfileResponse;
import com.lanyu.xiaolanaitravel.user.entity.UserProfile;
import com.lanyu.xiaolanaitravel.user.mapper.UserProfileMapper;
import org.springframework.stereotype.Service;

/** 用户旅行画像业务。 */
@Service
public class UserProfileService {
    private final UserProfileMapper userProfileMapper;

    public UserProfileService(UserProfileMapper userProfileMapper) {
        this.userProfileMapper = userProfileMapper;
    }

    public UserProfile getProfile(Long userId) {
        LambdaQueryWrapper<UserProfile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserProfile::getUserId, userId);
        return userProfileMapper.selectOne(wrapper);
    }

    /** 首次保存时新增画像，以后保存时更新同一条画像。 */
    public UserProfile saveProfile(Long userId, UserProfileRequest request) {
        UserProfile profile = getProfile(userId);
        if (profile == null) {
            profile = new UserProfile();
            profile.setUserId(userId);
            applyToEntity(profile, request);
            try {
                userProfileMapper.insert(profile);
                return getProfile(userId);
            } catch (DuplicateKeyException ignored) {
                // A concurrent first save won the unique user_id race; update it below.
            }
        }

        applyToEntity(profile, request);
        if (hasAnyField(request)) {
            userProfileMapper.updateById(profile);
        }
        return getProfile(userId);
    }

    public UserProfileResponse toResponse(UserProfile profile) {
        if (profile == null) {
            return new UserProfileResponse(null, null, null, null, null, null,
                    null, null, null, null, null, null);
        }
        return new UserProfileResponse(profile.getMbti(), profile.getTravelPace(),
                profile.getBudgetPreference(), profile.getTransportPreference(),
                profile.getInterestTags(), profile.getDislikeTags(), profile.getSpecialNotes(),
                profile.getCompanionPreference(), profile.getFoodPreference(),
                profile.getMealStylePreference(), profile.getRestaurantPreference(),
                profile.getAccommodationPreference());
    }

    private String normalize(String value) {
        return value.isBlank() ? null : value.strip();
    }

    private void applyToEntity(UserProfile profile, UserProfileRequest request) {
        if (request.getMbti() != null) profile.setMbti(normalize(request.getMbti()));
        if (request.getTravelPace() != null) profile.setTravelPace(normalize(request.getTravelPace()));
        if (request.getBudgetPreference() != null) profile.setBudgetPreference(normalize(request.getBudgetPreference()));
        if (request.getTransportPreference() != null) profile.setTransportPreference(normalize(request.getTransportPreference()));
        if (request.getInterestTags() != null) profile.setInterestTags(normalize(request.getInterestTags()));
        if (request.getDislikeTags() != null) profile.setDislikeTags(normalize(request.getDislikeTags()));
        if (request.getSpecialNotes() != null) profile.setSpecialNotes(normalize(request.getSpecialNotes()));
        if (request.getCompanionPreference() != null) profile.setCompanionPreference(normalize(request.getCompanionPreference()));
        if (request.getFoodPreference() != null) profile.setFoodPreference(normalize(request.getFoodPreference()));
        if (request.getMealStylePreference() != null) profile.setMealStylePreference(normalize(request.getMealStylePreference()));
        if (request.getRestaurantPreference() != null) profile.setRestaurantPreference(normalize(request.getRestaurantPreference()));
        if (request.getAccommodationPreference() != null) profile.setAccommodationPreference(normalize(request.getAccommodationPreference()));
    }

    private boolean hasAnyField(UserProfileRequest request) {
        return request.getMbti() != null || request.getTravelPace() != null
                || request.getBudgetPreference() != null || request.getTransportPreference() != null
                || request.getInterestTags() != null || request.getDislikeTags() != null
                || request.getSpecialNotes() != null || request.getCompanionPreference() != null
                || request.getFoodPreference() != null || request.getMealStylePreference() != null
                || request.getRestaurantPreference() != null
                || request.getAccommodationPreference() != null;
    }
}
