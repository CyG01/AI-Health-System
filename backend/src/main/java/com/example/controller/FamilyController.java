package com.example.controller;

import com.example.common.Result;
import com.example.entity.SysFamily;
import com.example.entity.SysFamilyInvitation;
import com.example.entity.SysFamilyUser;
import com.example.service.FamilyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 家庭组控制器
 */
@Tag(name = "家庭组管理", description = "家庭组创建、成员管理、邀请等功能")
@RestController
@RequestMapping("/api/v1/family")
@RequiredArgsConstructor
public class FamilyController {

    private final FamilyService familyService;

    @Operation(summary = "创建家庭组")
    @PostMapping("/create")
    public Result<SysFamily> createFamily(
            @RequestParam String familyName,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        SysFamily family = familyService.createFamily(userId, familyName);
        return Result.success(family);
    }

    @Operation(summary = "获取我的家庭列表")
    @GetMapping("/my-families")
    public Result<List<SysFamily>> getMyFamilies(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<SysFamily> families = familyService.getUserFamilies(userId);
        return Result.success(families);
    }

    @Operation(summary = "获取主家庭信息")
    @GetMapping("/primary")
    public Result<SysFamily> getPrimaryFamily(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        SysFamily family = familyService.getPrimaryFamily(userId);
        return Result.success(family);
    }

    @Operation(summary = "获取家庭成员列表")
    @GetMapping("/{familyId}/members")
    public Result<List<SysFamilyUser>> getFamilyMembers(
            @PathVariable Long familyId,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<SysFamilyUser> members = familyService.getFamilyMembers(familyId, userId);
        return Result.success(members);
    }

    @Operation(summary = "邀请成员加入家庭")
    @PostMapping("/{familyId}/invite")
    public Result<SysFamilyInvitation> inviteMember(
            @PathVariable Long familyId,
            @RequestParam String phone,
            @RequestParam(defaultValue = "MEMBER") String role,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        SysFamilyInvitation invitation = familyService.inviteMember(familyId, userId, phone, role);
        return Result.success(invitation);
    }

    @Operation(summary = "通过邀请码加入家庭")
    @PostMapping("/join")
    public Result<Map<String, Object>> joinFamily(
            @RequestParam String inviteCode,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Map<String, Object> result = familyService.joinFamilyByCode(userId, inviteCode);
        return Result.success(result);
    }

    @Operation(summary = "移除家庭成员")
    @DeleteMapping("/{familyId}/members/{memberId}")
    public Result<Boolean> removeMember(
            @PathVariable Long familyId,
            @PathVariable Long memberId,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        boolean success = familyService.removeMember(familyId, userId, memberId);
        return Result.success(success);
    }

    @Operation(summary = "更新成员角色")
    @PutMapping("/{familyId}/members/{memberId}/role")
    public Result<Boolean> updateMemberRole(
            @PathVariable Long familyId,
            @PathVariable Long memberId,
            @RequestParam String newRole,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        boolean success = familyService.updateMemberRole(familyId, userId, memberId, newRole);
        return Result.success(success);
    }

    @Operation(summary = "退出家庭")
    @PostMapping("/{familyId}/leave")
    public Result<Boolean> leaveFamily(
            @PathVariable Long familyId,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        boolean success = familyService.leaveFamily(familyId, userId);
        return Result.success(success);
    }

    @Operation(summary = "获取可查看的家庭成员ID列表（数据隔离用）")
    @GetMapping("/viewable-members")
    public Result<List<Long>> getViewableMemberIds(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<Long> viewableIds = familyService.getViewableMemberIds(userId);
        return Result.success(viewableIds);
    }
}
