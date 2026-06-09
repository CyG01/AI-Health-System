package com.example.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.Result;
import com.example.entity.SysAnnouncement;
import com.example.service.AdminAnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "公告查询")
@RestController
@RequestMapping("/api/announcement")
public class AnnouncementController {

    @Autowired
    private AdminAnnouncementService adminAnnouncementService;

    @Operation(summary = "公告列表")
    @GetMapping("/list")
    public Result<Page<SysAnnouncement>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(adminAnnouncementService.listAnnouncements(page, size));
    }

    @Operation(summary = "公告详情")
    @GetMapping("/{id}")
    public Result<SysAnnouncement> detail(@PathVariable Long id) {
        return Result.success(adminAnnouncementService.getById(id));
    }
}