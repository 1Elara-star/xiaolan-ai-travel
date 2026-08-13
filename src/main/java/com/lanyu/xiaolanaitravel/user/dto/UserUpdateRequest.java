package com.lanyu.xiaolanaitravel.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @Size(max = 50, message = "昵称不能超过50字") String nickname,
        @Size(max = 255, message = "头像地址不能超过255字") String avatar,
        @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确") String phone,
        @Email(message = "邮箱格式不正确")
        @Size(max = 100, message = "邮箱不能超过100字") String email) {
}
