package com.example.controller;

import com.example.annotation.NoRepeatSubmit;
import com.example.common.Result;
import com.example.dto.NotificationPreferenceDTO;
import com.example.entity.SysUser;
import com.example.mapper.SysUserMapper;
import com.example.vo.NotificationPreferenceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "通知偏好设置")
@RestController
@RequestMapping("/api/notification-preference")
@RequiredArgsConstructor
public class NotificationPreferenceController {

    private final SysUserMapper sysUserMapper;

    @Operation(summary = "获取通知偏好")
    @GetMapping
    public Result<NotificationPreferenceVO> getPreference(@RequestAttribute("userId") Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        NotificationPreferenceVO vo = new NotificationPreferenceVO();
        if (user != null) {
            vo.setNotificationEnabled(user.getNotificationEnabled());
            vo.setReminderTime(user.getReminderTime());
            vo.setNotifyExercise(user.getNotifyExercise());
            vo.setNotifyDiet(user.getNotifyDiet());
            vo.setNotifyCheckin(user.getNotifyCheckin());
            vo.setQuietStart(user.getQuietStart());
            vo.setQuietEnd(user.getQuietEnd());
        }
        return Result.success(vo);
    }

    @NoRepeatSubmit
    @Operation(summary = "更新通知偏好")
    @PutMapping
    public Result<Void> updatePreference(@RequestAttribute("userId") Long userId,
                                          @Valid @RequestBody NotificationPreferenceDTO dto) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user != null) {
            if (dto.getNotificationEnabled() != null) user.setNotificationEnabled(dto.getNotificationEnabled());
            if (dto.getReminderTime() != null) user.setReminderTime(dto.getReminderTime());
            if (dto.getNotifyExercise() != null) user.setNotifyExercise(dto.getNotifyExercise());
            if (dto.getNotifyDiet() != null) user.setNotifyDiet(dto.getNotifyDiet());
            if (dto.getNotifyCheckin() != null) user.setNotifyCheckin(dto.getNotifyCheckin());
            if (dto.getQuietStart() != null) user.setQuietStart(dto.getQuietStart());
            if (dto.getQuietEnd() != null) user.setQuietEnd(dto.getQuietEnd());
            sysUserMapper.updateById(user);
        }
        return Result.success();
    }
}