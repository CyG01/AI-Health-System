package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.BusinessException;
import com.example.dto.AnnouncementCreateDTO;
import com.example.dto.AnnouncementUpdateDTO;
import com.example.entity.SysAnnouncement;
import com.example.entity.SysNotification;
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

    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_PUBLISHED = 1;

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
    @Transactional(rollbackFor = Exception.class)
    public SysAnnouncement createAnnouncement(AnnouncementCreateDTO dto, Long adminId) {
        SysAnnouncement announcement = new SysAnnouncement();
        announcement.setTitle(dto.getTitle());
        announcement.setContent(dto.getContent());
        announcement.setAdminId(adminId);
        announcement.setStatus(STATUS_DRAFT);
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
        if (dto.getStatus() != null) {
            announcement.setStatus(dto.getStatus());
        }
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
        if (announcement.getStatus() != null && announcement.getStatus() == STATUS_PUBLISHED) {
            throw new BusinessException("公告已发布，无需重复操作");
        }
        announcement.setStatus(STATUS_PUBLISHED);
        announcement.setPublishTime(LocalDateTime.now());
        announcement.setUpdateTime(LocalDateTime.now());
        sysAnnouncementMapper.updateById(announcement);
        log.info("管理员发布公告 announcementId={}", id);

        broadcastNotificationAsync(announcement);
    }

    @Async
    public void broadcastNotificationAsync(SysAnnouncement announcement) {
        try {
            List<Long> userIds = sysUserMapper.selectList(null).stream()
                    .map(u -> u.getId())
                    .toList();

            for (Long userId : userIds) {
                SysNotification notification = new SysNotification();
                notification.setUserId(userId);
                notification.setTitle("系统公告");
                notification.setContent(announcement.getTitle());
                notification.setType("system");
                notification.setIsRead(0);
                sysNotificationMapper.insert(notification);
            }
            log.info("公告广播完成 announcementId={} 通知用户数={}", announcement.getId(), userIds.size());
        } catch (Exception e) {
            log.error("公告广播失败 announcementId={}", announcement.getId(), e);
        }
    }
}
