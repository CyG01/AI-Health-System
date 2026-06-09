package com.example.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.annotation.NoRepeatSubmit;
import com.example.common.Result;
import com.example.service.NotificationService;
import com.example.vo.NotificationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "系统通知")
@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Operation(summary = "获取未读通知数量")
    @GetMapping("/unread-count")
    public Result<Long> unreadCount(@RequestAttribute("userId") Long userId) {
        return Result.success(notificationService.getUnreadCount(userId));
    }

    @Operation(summary = "获取通知列表")
    @GetMapping("/list")
    public Result<Page<NotificationVO>> list(
            @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(notificationService.getNotificationList(userId, page, size));
    }

    @NoRepeatSubmit
    @Operation(summary = "标记单条通知已读")
    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id,
                                 @RequestAttribute("userId") Long userId) {
        notificationService.markAsRead(id, userId);
        return Result.success();
    }

    @NoRepeatSubmit
    @Operation(summary = "全部标记已读")
    @PutMapping("/read-all")
    public Result<Void> markAllRead(@RequestAttribute("userId") Long userId) {
        notificationService.markAllAsRead(userId);
        return Result.success();
    }

    @NoRepeatSubmit
    @Operation(summary = "删除通知")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id,
                               @RequestAttribute("userId") Long userId) {
        notificationService.deleteNotification(id, userId);
        return Result.success();
    }
}
