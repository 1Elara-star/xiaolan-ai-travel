package com.lanyu.xiaolanaitravel.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** 用户注册请求参数。 */
@Data
public class RegisterRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度需要在3-50个字符之间")
    @Pattern(regexp = "[A-Za-z0-9_.-]+", message = "用户名只能包含字母、数字、点、下划线和短横线")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 30, message = "密码长度需要在6-30个字符之间")
    @Pattern(regexp = "[\\x21-\\x7E]+", message = "密码只能使用可见英文字符")
    private String password;

    @Size(max = 50, message = "昵称不能超过50字")
    private String nickname;
}
