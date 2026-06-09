package com.example.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dto.AnnouncementCreateDTO;
import com.example.dto.AnnouncementUpdateDTO;
import com.example.entity.SysAnnouncement;

public interface AdminAnnouncementService {

    Page<SysAnnouncement> listAnnouncements(int page, int size);

    SysAnnouncement getById(Long id);

    SysAnnouncement createAnnouncement(AnnouncementCreateDTO dto, Long adminId);

    SysAnnouncement updateAnnouncement(AnnouncementUpdateDTO dto);

    void deleteAnnouncement(Long id);

    void publishAnnouncement(Long id);
}
