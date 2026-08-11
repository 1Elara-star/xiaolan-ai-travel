package com.lanyu.xiaolanaitravel.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 当前登录用户信息
 */
@Data
@AllArgsConstructor
public class UserInfoResponse {

    private Long id;

    private String username;

    private String nickname;

    private String avatar;

    private String phone;

    private String email;

    private String role;
}