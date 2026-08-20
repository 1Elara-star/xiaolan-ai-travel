package com.lanyu.xiaolanaitravel.user.controller;

import com.lanyu.xiaolanaitravel.user.dto.AdminAttractionResponse;
import com.lanyu.xiaolanaitravel.user.dto.AdminOverviewResponse;
import com.lanyu.xiaolanaitravel.user.dto.AdminPlanResponse;
import com.lanyu.xiaolanaitravel.user.dto.AdminRoleUpdateRequest;
import com.lanyu.xiaolanaitravel.user.dto.AdminUserResponse;
import com.lanyu.xiaolanaitravel.user.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/overview")
    public AdminOverviewResponse overview() {
        return adminService.overview();
    }

    @GetMapping("/users")
    public List<AdminUserResponse> users(@RequestParam(required = false) String keyword) {
        return adminService.users(keyword);
    }

    @PutMapping("/users/{userId}/role")
    public AdminUserResponse updateRole(
            @RequestAttribute("currentUserId") Long currentAdminId,
            @PathVariable Long userId,
            @Valid @RequestBody AdminRoleUpdateRequest request) {
        return adminService.updateRole(currentAdminId, userId, request.role());
    }

    @GetMapping("/plans")
    public List<AdminPlanResponse> plans(@RequestParam(required = false) String keyword) {
        return adminService.plans(keyword);
    }

    @GetMapping("/attractions")
    public List<AdminAttractionResponse> attractions(@RequestParam(required = false) String keyword) {
        return adminService.attractions(keyword);
    }
}
