package com.example.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dto.SendNotificationDTO;
import com.example.vo.NotificationVO;

public interface NotificationService {

    Page<NotificationVO> getNotificationList(Long userId, int page, int size);

    long getUnreadCount(Long userId);

    void markAsRead(Long notificationId, Long userId);

    void markAllAsRead(Long userId);

    void deleteNotification(Long notificationId, Long userId);

    void sendNotification(SendNotificationDTO dto);
}
