package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.BusinessException;
import com.example.convert.NotificationConvert;
import com.example.entity.SysNotification;
import com.example.mapper.SysNotificationMapper;
import com.example.service.NotificationService;
import com.example.vo.NotificationVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    @Autowired
    private SysNotificationMapper sysNotificationMapper;

    @Autowired
    private NotificationConvert notificationConvert;

    @Override
    public Page<NotificationVO> getNotificationList(Long userId, int page, int size) {
        Page<SysNotification> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SysNotification> wrapper = new LambdaQueryWrapper<SysNotification>()
                .eq(SysNotification::getUserId, userId)
                .orderByDesc(SysNotification::getCreateTime);
        Page<SysNotification> result = sysNotificationMapper.selectPage(pageParam, wrapper);
        Page<NotificationVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(notificationConvert.toNotificationVOList(result.getRecords()));
        return voPage;
    }

    @Override
    public long getUnreadCount(Long userId) {
        LambdaQueryWrapper<SysNotification> wrapper = new LambdaQueryWrapper<SysNotification>()
                .eq(SysNotification::getUserId, userId)
                .eq(SysNotification::getIsRead, 0);
        return sysNotificationMapper.selectCount(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long notificationId, Long userId) {
        SysNotification notification = sysNotificationMapper.selectById(notificationId);
        if (notification == null) {
            throw new BusinessException(404, "通知不存在");
        }
        if (!notification.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作此通知");
        }
        notification.setIsRead(1);
        notification.setReadTime(LocalDateTime.now());
        sysNotificationMapper.updateById(notification);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllAsRead(Long userId) {
        LambdaUpdateWrapper<SysNotification> wrapper = new LambdaUpdateWrapper<SysNotification>()
                .eq(SysNotification::getUserId, userId)
                .eq(SysNotification::getIsRead, 0)
                .set(SysNotification::getIsRead, 1)
                .set(SysNotification::getReadTime, LocalDateTime.now());
        sysNotificationMapper.update(null, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNotification(Long notificationId, Long userId) {
        SysNotification notification = sysNotificationMapper.selectById(notificationId);
        if (notification == null) {
            throw new BusinessException(404, "通知不存在");
        }
        if (!notification.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作此通知");
        }
        sysNotificationMapper.deleteById(notificationId);
        log.info("用户删除通知 userId={} notificationId={}", userId, notificationId);
    }
}
