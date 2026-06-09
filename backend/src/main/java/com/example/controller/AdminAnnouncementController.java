package com.example.controller;

import com.example.annotation.AdminOnly;
import com.example.annotation.NoRepeatSubmit;
import com.example.common.Result;
import com.example.dto.AnnouncementCreateDTO;
import com.example.dto.AnnouncementUpdateDTO;
import com.example.entity.SysAnnouncement;
import com.example.service.AdminAnnouncementService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理员公告管理")
@AdminOnly
@RestController
@RequestMapping("/api/admin/announcement")
public class AdminAnnouncementController {

    @Autowired
    private AdminAnnouncementService adminAnnouncementService;

    @Operation(summary = "公告列表")
    @GetMapping("/list")
    public Result<Page<SysAnnouncement>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(adminAnnouncementService.listAnnouncements(page, size));
    }

    @NoRepeatSubmit
    @Operation(summary = "创建公告")
    @PostMapping
    public Result<Void> create(@Validated @RequestBody AnnouncementCreateDTO dto,
                               @RequestAttribute("userId") Long userId) {
        adminAnnouncementService.createAnnouncement(dto, userId);
        return Result.success();
    }

    @NoRepeatSubmit
    @Operation(summary = "修改公告")
    @PutMapping
    public Result<Void> update(@Validated @RequestBody AnnouncementUpdateDTO dto) {
        adminAnnouncementService.updateAnnouncement(dto);
        return Result.success();
    }

    @NoRepeatSubmit
    @Operation(summary = "删除公告")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminAnnouncementService.deleteAnnouncement(id);
        return Result.success();
    }

    @NoRepeatSubmit
    @Operation(summary = "发布公告")
    @PutMapping("/{id}/publish")
    public Result<Void> publish(@PathVariable Long id) {
        adminAnnouncementService.publishAnnouncement(id);
        return Result.success();
    }
}
