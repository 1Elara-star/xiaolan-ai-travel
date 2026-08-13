package com.lanyu.xiaolanaitravel.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lanyu.xiaolanaitravel.common.util.JwtUtil;
import com.lanyu.xiaolanaitravel.user.dto.*;
import com.lanyu.xiaolanaitravel.user.entity.User;
import com.lanyu.xiaolanaitravel.user.mapper.UserMapper;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** 用户注册、登录和个人信息业务。 */
@Service
public class UserService {
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserMapper userMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
    }

    public boolean register(RegisterRequest request) {
        String username = request.getUsername().strip();
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        if (userMapper.selectCount(wrapper) > 0) {
            return false;
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(normalize(request.getNickname()));
        user.setRole("USER");
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException exception) {
            // The unique username index closes the select-then-insert race.
            return false;
        }
        return true;
    }

    public LoginResponse login(LoginRequest request) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername().strip());
        User user = userMapper.selectOne(wrapper);
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        return new LoginResponse(jwtUtil.generateToken(user), user.getId(), user.getUsername(),
                user.getNickname(), user.getRole());
    }

    public UserInfoResponse getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在");
        }
        return new UserInfoResponse(user.getId(), user.getUsername(), user.getNickname(),
                user.getAvatar(), user.getPhone(), user.getEmail(), user.getRole());
    }

    public UserInfoResponse updateUserInfo(Long userId, UserUpdateRequest request) {
        var update = new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<User>();
        update.eq(User::getId, userId);
        boolean supplied = false;
        if (request.nickname() != null) {
            update.set(User::getNickname, normalize(request.nickname()));
            supplied = true;
        }
        if (request.avatar() != null) {
            update.set(User::getAvatar, normalize(request.avatar()));
            supplied = true;
        }
        if (request.phone() != null) {
            update.set(User::getPhone, normalize(request.phone()));
            supplied = true;
        }
        if (request.email() != null) {
            update.set(User::getEmail, normalize(request.email()));
            supplied = true;
        }
        if (supplied && userMapper.update(null, update) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在");
        }
        return getUserInfo(userId);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
