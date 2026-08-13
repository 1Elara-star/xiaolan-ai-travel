package com.lanyu.xiaolanaitravel.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 用户登录请求参数。 */
@Data
public class LoginRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名不能超过50个字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(max = 100, message = "密码过长")
    private String password;
}
