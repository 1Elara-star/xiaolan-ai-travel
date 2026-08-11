package com.lanyu.xiaolanaitravel.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 用户登录成功后的返回数据
 */
@Data
@AllArgsConstructor
public class LoginResponse {

    /**
     * JWT令牌
     */
    private String token;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 用户角色：USER / ADMIN
     */
    private String role;
}