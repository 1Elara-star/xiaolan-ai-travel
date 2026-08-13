package com.lanyu.xiaolanaitravel.user.controller;

import com.lanyu.xiaolanaitravel.user.dto.UserProfileRequest;
import com.lanyu.xiaolanaitravel.user.dto.UserProfileResponse;
import com.lanyu.xiaolanaitravel.user.service.UserProfileService;
import jakarta.validation.Valid;
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
    public UserProfileResponse getProfile(@RequestAttribute("currentUserId") Long userId) {
        return userProfileService.toResponse(userProfileService.getProfile(userId));
    }

    @PutMapping
    public UserProfileResponse saveProfile(@RequestAttribute("currentUserId") Long userId,
                                   @Valid @RequestBody UserProfileRequest request) {
        return userProfileService.toResponse(userProfileService.saveProfile(userId, request));
    }
}
