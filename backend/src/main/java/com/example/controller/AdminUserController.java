package com.example.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.annotation.AdminOnly;
import com.example.annotation.NoRepeatSubmit;
import com.example.common.Result;
import com.example.service.AdminUserService;
import com.example.vo.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理员用户管理")
@AdminOnly
@RestController
@RequestMapping("/api/admin/user")
public class AdminUserController {

    @Autowired
    private AdminUserService adminUserService;

    @Operation(summary = "分页查询用户列表")
    @GetMapping("/list")
    public Result<Page<UserInfoVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return Result.success(adminUserService.listUsers(page, size, keyword));
    }

    @NoRepeatSubmit
    @Operation(summary = "禁用用户")
    @PutMapping("/{id}/ban")
    public Result<Void> ban(@PathVariable Long id) {
        adminUserService.banUser(id);
        return Result.success();
    }

    @NoRepeatSubmit
    @Operation(summary = "启用用户")
    @PutMapping("/{id}/unban")
    public Result<Void> unban(@PathVariable Long id) {
        adminUserService.unbanUser(id);
        return Result.success();
    }
}
