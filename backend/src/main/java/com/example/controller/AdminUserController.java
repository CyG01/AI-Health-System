package com.example.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.annotation.AdminOnly;
import com.example.annotation.NoRepeatSubmit;
import com.example.common.Result;
import com.example.service.AdminUserService;
import com.example.service.AuditLogService;
import com.example.vo.AdminUserDetailVO;
import com.example.vo.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "管理员用户管理")
@AdminOnly
@RestController
@RequestMapping("/api/admin/user")
public class AdminUserController {

    @Autowired
    private AdminUserService adminUserService;

    @Autowired
    private AuditLogService auditLogService;

    @Operation(summary = "分页查询用户列表（支持状态/日期筛选）")
    @GetMapping("/list")
    public Result<Page<UserInfoVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.success(adminUserService.listUsers(page, size, keyword, status, startDate, endDate));
    }

    @Operation(summary = "用户详情（含档案/计划/打卡/运动/饮食统计）")
    @GetMapping("/{id}/detail")
    public Result<AdminUserDetailVO> detail(@PathVariable Long id,
                                            @RequestAttribute("userId") Long userId,
                                            HttpServletRequest request) {
        auditLogService.log(userId, null, "VIEW", "user", id,
                "查看用户详情", request.getRemoteAddr());
        return Result.success(adminUserService.getUserDetail(id));
    }

    @Operation(summary = "导出用户列表 CSV")
    @GetMapping("/export")
    public Result<List<UserInfoVO>> export(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestAttribute("userId") Long userId,
            HttpServletRequest request) {
        auditLogService.log(userId, null, "EXPORT", "user", null,
                "导出用户列表", request.getRemoteAddr());
        return Result.success(adminUserService.exportUsers(keyword, status, startDate, endDate));
    }

    @NoRepeatSubmit
    @Operation(summary = "禁用用户")
    @PutMapping("/{id}/ban")
    public Result<Void> ban(@PathVariable Long id,
                            @RequestAttribute("userId") Long userId,
                            HttpServletRequest request) {
        adminUserService.banUser(id);
        auditLogService.log(userId, null, "BAN", "user", id,
                "禁用用户", request.getRemoteAddr());
        return Result.success();
    }

    @NoRepeatSubmit
    @Operation(summary = "启用用户")
    @PutMapping("/{id}/unban")
    public Result<Void> unban(@PathVariable Long id,
                              @RequestAttribute("userId") Long userId,
                              HttpServletRequest request) {
        adminUserService.unbanUser(id);
        auditLogService.log(userId, null, "UNBAN", "user", id,
                "启用用户", request.getRemoteAddr());
        return Result.success();
    }
}
