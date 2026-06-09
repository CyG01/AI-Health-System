package com.example.controller;

import com.example.annotation.NoRepeatSubmit;
import com.example.common.Result;
import com.example.convert.UserConvert;
import com.example.dto.UpdatePasswordDTO;
import com.example.dto.UpdateProfileDTO;
import com.example.service.UserService;
import com.example.vo.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Tag(name = "个人中心")
@RestController
@RequestMapping("/api/auth")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Value("${app.upload.avatar-dir:uploads/avatars}")
    private String avatarDir;

    @Autowired
    private UserService userService;

    @Autowired
    private UserConvert userConvert;

    @NoRepeatSubmit
    @Operation(summary = "修改密码")
    @PutMapping("/update-password")
    public Result<Void> updatePassword(@Validated @RequestBody UpdatePasswordDTO dto,
                                       @RequestAttribute("userId") Long userId) {
        userService.updatePassword(userId, dto);
        return Result.success();
    }

    @Operation(summary = "获取个人信息")
    @GetMapping("/profile")
    public Result<UserInfoVO> getProfile(@RequestAttribute("userId") Long userId) {
        return Result.success(userService.getProfile(userId));
    }

    @NoRepeatSubmit
    @Operation(summary = "更新个人信息")
    @PutMapping("/update-profile")
    public Result<UserInfoVO> updateProfile(@Validated @RequestBody UpdateProfileDTO dto,
                                            @RequestAttribute("userId") Long userId) {
        return Result.success(userService.updateProfile(userId, dto));
    }

    @NoRepeatSubmit
    @Operation(summary = "上传头像")
    @PostMapping("/upload-avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file,
                                        @RequestAttribute("userId") Long userId) {
        if (file.isEmpty()) {
            return Result.error(400, "文件不能为空");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error(400, "只允许上传图片文件");
        }
        if (file.getSize() > 2 * 1024 * 1024) {
            return Result.error(400, "头像大小不能超过2MB");
        }

        try {
            Path uploadPath = Paths.get(avatarDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalName = file.getOriginalFilename();
            String extension = "";
            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf("."));
            }
            String fileName = userId + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
            Path filePath = uploadPath.resolve(fileName);
            file.transferTo(filePath.toFile());

            // 删除旧头像文件
            String oldAvatarUrl = userService.getAvatar(userId);
            if (oldAvatarUrl != null && oldAvatarUrl.startsWith("/avatars/")) {
                Path oldFilePath = Paths.get(avatarDir, oldAvatarUrl.substring("/avatars/".length()));
                try {
                    Files.deleteIfExists(oldFilePath);
                } catch (IOException ignored) {
                    log.warn("删除旧头像文件失败 userId={} path={}", userId, oldFilePath);
                }
            }

            String avatarUrl = "/avatars/" + fileName;
            userService.updateAvatar(userId, avatarUrl);
            log.info("头像上传成功 userId={} avatarUrl={}", userId, avatarUrl);
            return Result.success(avatarUrl);
        } catch (IOException e) {
            log.error("头像上传失败 userId={}", userId, e);
            return Result.error(500, "头像上传失败");
        }
    }
}
