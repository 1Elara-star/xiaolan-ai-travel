package com.lanyu.xiaolanaitravel.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lanyu.xiaolanaitravel.user.dto.UserProfileRequest;
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
        }
        profile.setMbti(request.getMbti());
        profile.setTravelPace(request.getTravelPace());
        profile.setBudgetPreference(request.getBudgetPreference());
        profile.setTransportPreference(request.getTransportPreference());
        profile.setInterestTags(request.getInterestTags());
        profile.setDislikeTags(request.getDislikeTags());
        profile.setSpecialNotes(request.getSpecialNotes());
        profile.setCompanionPreference(request.getCompanionPreference());
        profile.setFoodPreference(request.getFoodPreference());
        profile.setMealStylePreference(request.getMealStylePreference());
        profile.setRestaurantPreference(request.getRestaurantPreference());
        profile.setAccommodationPreference(request.getAccommodationPreference());

        if (profile.getId() == null) {
            userProfileMapper.insert(profile);
        } else {
            userProfileMapper.updateById(profile);
        }
        return profile;
    }
}
