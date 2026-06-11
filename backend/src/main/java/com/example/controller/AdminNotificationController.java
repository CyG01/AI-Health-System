package com.example.controller;

import com.example.annotation.AdminOnly;
import com.example.annotation.NoRepeatSubmit;
import com.example.common.Result;
import com.example.dto.SendNotificationDTO;
import com.example.service.AdminApprovalService;
import com.example.service.AuditLogService;
import com.example.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理员通知管理")
@AdminOnly
@RestController
@RequestMapping("/api/admin/notification")
public class AdminNotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private AdminApprovalService approvalService;

    @NoRepeatSubmit
    @Operation(summary = "发送通知（广播类通知需审批）")
    @PostMapping("/send")
    public Result<?> send(@Validated @RequestBody SendNotificationDTO dto,
                          @RequestAttribute("userId") Long userId,
                          @RequestHeader(value = "X-Approval-Id", required = false) Long approvalId,
                          HttpServletRequest request) {
        if (!approvalService.checkApproval("send_notification", approvalId, userId)) {
            return Result.error(403, "全站发送通知为敏感操作，请先发起审批申请: POST /api/admin/approvals/request");
        }
        notificationService.sendNotification(dto);
        auditLogService.log(userId, null, "SEND_NOTIFICATION", "notification", null,
                "发送通知: " + dto.getTitle() + " [审批ID:" + approvalId + "]", request.getRemoteAddr());
        approvalService.markExecuted(approvalId);
        return Result.success();
    }
}