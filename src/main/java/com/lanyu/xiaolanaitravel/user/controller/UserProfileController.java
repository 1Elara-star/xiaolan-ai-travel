package com.lanyu.xiaolanaitravel.user.controller;

import com.lanyu.xiaolanaitravel.user.dto.UserProfileRequest;
import com.lanyu.xiaolanaitravel.user.entity.UserProfile;
import com.lanyu.xiaolanaitravel.user.service.UserProfileService;
import org.springframework.web.bind.annotation.*;

/** 用户旅行画像查询与保存接口。 */
@RestController
@RequestMapping("/user/profile")
public class UserProfileController {
    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping
    public UserProfile getProfile(@RequestAttribute("currentUserId") Long userId) {
        return userProfileService.getProfile(userId);
    }

    @PutMapping
    public UserProfile saveProfile(@RequestAttribute("currentUserId") Long userId,
                                   @RequestBody UserProfileRequest request) {
        return userProfileService.saveProfile(userId, request);
    }
}
