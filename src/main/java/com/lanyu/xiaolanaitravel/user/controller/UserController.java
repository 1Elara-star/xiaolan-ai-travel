package com.lanyu.xiaolanaitravel.user.controller;

import com.lanyu.xiaolanaitravel.user.dto.LoginRequest;
import com.lanyu.xiaolanaitravel.user.dto.LoginResponse;
import com.lanyu.xiaolanaitravel.user.dto.RegisterRequest;
import com.lanyu.xiaolanaitravel.user.dto.UserInfoResponse;
import com.lanyu.xiaolanaitravel.user.dto.UserUpdateRequest;
import com.lanyu.xiaolanaitravel.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** 用户注册、登录和个人信息接口。 */
@RestController
@RequestMapping("/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        if (!userService.register(request)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("用户名已经存在");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body("注册成功");
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    @GetMapping("/me")
    public UserInfoResponse getCurrentUser(@RequestAttribute("currentUserId") Long userId) {
        return userService.getUserInfo(userId);
    }

    @PutMapping("/me")
    public UserInfoResponse updateCurrentUser(
            @RequestAttribute("currentUserId") Long userId,
            @Valid @RequestBody UserUpdateRequest request) {
        return userService.updateUserInfo(userId, request);
    }
}
