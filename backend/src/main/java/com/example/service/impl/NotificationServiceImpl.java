package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.BusinessException;
import com.example.convert.NotificationConvert;
import com.example.dto.SendNotificationDTO;
import com.example.entity.SysNotification;
import com.example.entity.SysUser;
import com.example.mapper.SysNotificationMapper;
import com.example.mapper.SysUserMapper;
import com.example.service.NotificationService;
import com.example.vo.NotificationVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    @Autowired
    private SysNotificationMapper sysNotificationMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

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
        sysNotificationMapper.updateById(notification);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllAsRead(Long userId) {
        LambdaUpdateWrapper<SysNotification> wrapper = new LambdaUpdateWrapper<SysNotification>()
                .eq(SysNotification::getUserId, userId)
                .eq(SysNotification::getIsRead, 0)
                .set(SysNotification::getIsRead, 1);
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendNotification(SendNotificationDTO dto) {
        if (dto.isSendToAll()) {
            List<SysUser> allUsers = sysUserMapper.selectList(null);
            String notificationType = dto.getType() != null ? dto.getType() : "system";

            List<SysNotification> notifications = new ArrayList<>(allUsers.size());
            for (SysUser user : allUsers) {
                SysNotification notification = new SysNotification();
                notification.setUserId(user.getId());
                notification.setTitle(dto.getTitle());
                notification.setContent(dto.getContent());
                notification.setType(notificationType);
                notification.setIsRead(0);
                notifications.add(notification);
            }

            // 分批插入，每批500条，避免SQL语句过长
            int batchSize = 500;
            for (int i = 0; i < notifications.size(); i += batchSize) {
                int end = Math.min(i + batchSize, notifications.size());
                sysNotificationMapper.batchInsert(notifications.subList(i, end));
            }
            log.info("管理员向全体用户发送通知 title={} count={}", dto.getTitle(), allUsers.size());
        } else {
            if (dto.getUserId() == null) {
                throw new BusinessException("指定用户通知需要提供userId");
            }
            SysNotification notification = new SysNotification();
            notification.setUserId(dto.getUserId());
            notification.setTitle(dto.getTitle());
            notification.setContent(dto.getContent());
            notification.setType(dto.getType() != null ? dto.getType() : "system");
            notification.setIsRead(0);
            sysNotificationMapper.insert(notification);
            log.info("管理员向用户发送通知 title={} userId={}", dto.getTitle(), dto.getUserId());
        }
    }
}
