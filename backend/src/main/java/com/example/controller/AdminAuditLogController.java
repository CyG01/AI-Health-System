package com.example.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.annotation.AdminOnly;
import com.example.common.Result;
import com.example.entity.AdminAuditLog;
import com.example.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理员审计日志")
@AdminOnly
@RestController
@RequestMapping("/api/admin/audit-log")
public class AdminAuditLogController {

    @Autowired
    private AuditLogService auditLogService;

    @Operation(summary = "分页查询审计日志")
    @GetMapping("/page")
    public Result<Page<AdminAuditLog>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String operatorName) {
        return Result.success(auditLogService.queryPage(page, size, action, operatorName));
    }
}