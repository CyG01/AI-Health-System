package com.example.controller;

import com.example.annotation.AdminOnly;
import com.example.common.Result;
import com.example.entity.AdminApproval;
import com.example.service.AdminApprovalService;
import com.example.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理员审批流程接口。
 * 管理员可查看、审批或拒绝敏感操作申请。
 */
@AdminOnly
@RestController
@RequestMapping("/api/admin/approvals")
@RequiredArgsConstructor
public class AdminApprovalController {

    private final AdminApprovalService approvalService;
    private final AuditLogService auditLogService;

    /**
     * 获取待审批列表。
     */
    @GetMapping("/pending")
    public Result<List<AdminApproval>> getPendingApprovals() {
        List<AdminApproval> list = approvalService.getPendingApprovals();
        return Result.success(list);
    }

    /**
     * 通过审批。
     */
    @PostMapping("/{id}/approve")
    public Result<AdminApproval> approve(@PathVariable Long id,
                                          @RequestBody Map<String, String> body,
                                          @RequestHeader("X-Admin-Id") Long adminId) {
        String approverName = body.get("approverName");
        String reason = body.getOrDefault("reason", "");
        AdminApproval approval = approvalService.approve(id, adminId, approverName, reason);
        if (approval != null) {
            auditLogService.log(adminId, approverName, "APPROVE_OPERATION",
                    "approval", id, "审批通过: action=" + approval.getActionType(), null);
            return Result.success(approval);
        }
        return Result.error(400, "审批不存在或已处理");
    }

    /**
     * 拒绝审批。
     */
    @PostMapping("/{id}/reject")
    public Result<AdminApproval> reject(@PathVariable Long id,
                                         @RequestBody Map<String, String> body,
                                         @RequestHeader("X-Admin-Id") Long adminId) {
        String approverName = body.get("approverName");
        String reason = body.getOrDefault("reason", "");
        AdminApproval approval = approvalService.reject(id, adminId, approverName, reason);
        if (approval != null) {
            auditLogService.log(adminId, approverName, "REJECT_OPERATION",
                    "approval", id, "审批拒绝: action=" + approval.getActionType(), null);
            return Result.success(approval);
        }
        return Result.error(400, "审批不存在或已处理");
    }

    /**
     * 发起审批申请（批量修改用户数据等敏感操作前端调用）。
     */
    @PostMapping("/request")
    public Result<Map<String, Object>> requestApproval(@RequestBody Map<String, String> body,
                                                        @RequestHeader("X-Admin-Id") Long adminId) {
        String operatorName = body.getOrDefault("operatorName", "admin#" + adminId);
        String actionType = body.get("actionType");
        String targetDescription = body.get("targetDescription");
        String requestPayload = body.getOrDefault("requestPayload", "");

        if (actionType == null || targetDescription == null) {
            return Result.error(400, "actionType 和 targetDescription 不能为空");
        }

        Long approvalId = approvalService.requestApproval(
                adminId, operatorName, actionType, targetDescription, requestPayload);

        auditLogService.log(adminId, operatorName, "REQUEST_APPROVAL",
                "approval", null, "发起审批: action=" + actionType + " target=" + targetDescription, null);

        return Result.success(Map.of("approvalId", approvalId, "message", "审批申请已提交，请等待审批"));
    }
}