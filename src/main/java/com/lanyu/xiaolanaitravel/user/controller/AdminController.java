package com.lanyu.xiaolanaitravel.user.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 管理员权限测试接口。 */
@RestController
@RequestMapping("/admin")
public class AdminController {
    @GetMapping("/test")
    public String test() {
        return "管理员权限验证成功";
    }
}
