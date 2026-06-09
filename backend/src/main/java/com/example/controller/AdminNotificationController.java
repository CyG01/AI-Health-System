package com.example.controller;

import com.example.annotation.AdminOnly;
import com.example.annotation.NoRepeatSubmit;
import com.example.common.Result;
import com.example.dto.SendNotificationDTO;
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

    @NoRepeatSubmit
    @Operation(summary = "发送通知")
    @PostMapping("/send")
    public Result<Void> send(@Validated @RequestBody SendNotificationDTO dto,
                             @RequestAttribute("userId") Long userId,
                             HttpServletRequest request) {
        notificationService.sendNotification(dto);
        auditLogService.log(userId, null, "SEND_NOTIFICATION", "notification", null,
                "发送通知: " + dto.getTitle(), request.getRemoteAddr());
        return Result.success();
    }
}