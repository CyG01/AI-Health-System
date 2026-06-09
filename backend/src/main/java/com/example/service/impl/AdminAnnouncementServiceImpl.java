package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.BusinessException;
import com.example.dto.AnnouncementCreateDTO;
import com.example.dto.AnnouncementUpdateDTO;
import com.example.entity.SysAnnouncement;
import com.example.entity.SysNotification;
import com.example.entity.SysUser;
import com.example.mapper.SysAnnouncementMapper;
import com.example.mapper.SysNotificationMapper;
import com.example.mapper.SysUserMapper;
import com.example.service.AdminAnnouncementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminAnnouncementServiceImpl implements AdminAnnouncementService {

    private static final Logger log = LoggerFactory.getLogger(AdminAnnouncementServiceImpl.class);

    @Autowired
    private SysAnnouncementMapper sysAnnouncementMapper;

    @Autowired
    private SysNotificationMapper sysNotificationMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Override
    public Page<SysAnnouncement> listAnnouncements(int page, int size) {
        Page<SysAnnouncement> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SysAnnouncement> wrapper = new LambdaQueryWrapper<SysAnnouncement>()
                .orderByDesc(SysAnnouncement::getCreateTime);
        return sysAnnouncementMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public SysAnnouncement getById(Long id) {
        SysAnnouncement announcement = sysAnnouncementMapper.selectById(id);
        if (announcement == null) {
            throw new BusinessException(404, "公告不存在");
        }
        return announcement;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysAnnouncement createAnnouncement(AnnouncementCreateDTO dto, Long adminId) {
        SysAnnouncement announcement = new SysAnnouncement();
        announcement.setTitle(dto.getTitle());
        announcement.setContent(dto.getContent());
        announcement.setAdminId(adminId);
        sysAnnouncementMapper.insert(announcement);
        log.info("管理员创建公告 adminId={} announcementId={}", adminId, announcement.getId());
        return announcement;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysAnnouncement updateAnnouncement(AnnouncementUpdateDTO dto) {
        SysAnnouncement announcement = sysAnnouncementMapper.selectById(dto.getId());
        if (announcement == null) {
            throw new BusinessException(404, "公告不存在");
        }
        announcement.setTitle(dto.getTitle());
        announcement.setContent(dto.getContent());
        announcement.setUpdateTime(LocalDateTime.now());
        sysAnnouncementMapper.updateById(announcement);
        log.info("管理员更新公告 announcementId={}", dto.getId());
        return announcement;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAnnouncement(Long id) {
        SysAnnouncement announcement = sysAnnouncementMapper.selectById(id);
        if (announcement == null) {
            throw new BusinessException(404, "公告不存在");
        }
        sysAnnouncementMapper.deleteById(id);
        log.info("管理员删除公告 announcementId={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishAnnouncement(Long id) {
        SysAnnouncement announcement = sysAnnouncementMapper.selectById(id);
        if (announcement == null) {
            throw new BusinessException(404, "公告不存在");
        }
        broadcastNotificationAsync(announcement);
        log.info("管理员广播公告 announcementId={}", id);
    }

    @Async
    public void broadcastNotificationAsync(SysAnnouncement announcement) {
        try {
            int batchSize = 500;
            int pageNum = 1;
            int totalSent = 0;

            while (true) {
                Page<SysUser> userPage = new Page<>(pageNum, batchSize);
                Page<SysUser> result = sysUserMapper.selectPage(userPage, null);
                List<SysUser> users = result.getRecords();

                if (users.isEmpty()) {
                    break;
                }

                List<SysNotification> notifications = users.stream().map(user -> {
                    SysNotification notification = new SysNotification();
                    notification.setUserId(user.getId());
                    notification.setTitle("系统公告");
                    notification.setContent(announcement.getTitle());
                    notification.setType("system");
                    notification.setIsRead(0);
                    return notification;
                }).toList();

                // 批量插入
                for (SysNotification notif : notifications) {
                    sysNotificationMapper.insert(notif);
                }

                totalSent += users.size();
                pageNum++;

                if (users.size() < batchSize) {
                    break;
                }
            }

            log.info("公告广播完成 announcementId={} 通知用户数={}", announcement.getId(), totalSent);
        } catch (Exception e) {
            log.error("公告广播失败 announcementId={}", announcement.getId(), e);
        }
    }
}