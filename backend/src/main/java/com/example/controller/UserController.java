package com.example.controller;

import com.example.annotation.NoRepeatSubmit;
import com.example.common.Result;
import com.example.convert.UserConvert;
import com.example.dto.NotificationPreferenceDTO;
import com.example.dto.UpdatePasswordDTO;
import com.example.dto.UpdateProfileDTO;
import com.example.service.AuthService;
import com.example.service.UserService;
import com.example.vo.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Tag(name = "个人中心")
@RestController
@RequestMapping("/api/user")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    /** 允许的图片扩展名（小写） */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp");

    /** 常见图片格式的魔数签名 */
    private static final Map<String, byte[]> IMAGE_MAGIC_BYTES = Map.of(
            "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
            "image/png",  new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47},
            "image/gif",  new byte[]{0x47, 0x49, 0x46, 0x38},
            "image/bmp",  new byte[]{0x42, 0x4D},
            "image/webp", new byte[]{0x52, 0x49, 0x46, 0x46}
    );

    @Value("${app.upload.avatar-dir:uploads/avatars}")
    private String avatarDir;

    @Autowired
    private UserService userService;
    @Autowired
    private AuthService authService;

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
        // 魔数校验：防止将非图片文件伪装为图片上传
        if (!verifyImageMagicBytes(file, contentType)) {
            return Result.error(400, "文件格式与声明类型不匹配，请上传真实图片");
        }
        // 扩展名白名单校验
        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
        }
        if (!extension.isEmpty() && !ALLOWED_EXTENSIONS.contains(extension)) {
            return Result.error(400, "不支持的图片格式，仅允许 jpg/png/gif/bmp/webp");
        }

        try {
            Path uploadPath = Paths.get(avatarDir).toAbsolutePath().normalize();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String fileName = userId + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
            Path filePath = uploadPath.resolve(fileName).normalize();
            // 防御路径穿越：确保最终路径在 uploadPath 范围内
            if (!filePath.startsWith(uploadPath)) {
                log.error("路径穿越攻击尝试 userId={} path={}", userId, filePath);
                return Result.error(400, "非法文件路径");
            }
            file.transferTo(filePath.toFile());

            // 删除旧头像文件（防御路径穿越）
            String oldAvatarUrl = userService.getAvatar(userId);
            if (oldAvatarUrl != null && oldAvatarUrl.startsWith("/avatars/")) {
                String relative = oldAvatarUrl.substring("/avatars/".length());
                // 校验不包含路径穿越字符
                if (!relative.contains("..") && !relative.contains("/") && !relative.contains("\\")) {
                    try {
                        Path oldFilePath = uploadPath.resolve(relative).normalize();
                        if (oldFilePath.startsWith(uploadPath)) {
                            Files.deleteIfExists(oldFilePath);
                        }
                    } catch (IOException | InvalidPathException ignored) {
                        log.warn("删除旧头像文件失败 userId={} path={}", userId, relative);
                    }
                } else {
                    log.warn("旧头像路径异常，跳过删除 userId={} url={}", userId, oldAvatarUrl);
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

    /**
     * 校验文件开头的魔数字节是否与声明的 Content-Type 匹配。
     */
    private boolean verifyImageMagicBytes(MultipartFile file, String declaredContentType) {
        byte[] expected = IMAGE_MAGIC_BYTES.get(declaredContentType);
        if (expected == null) {
            return false;
        }
        try {
            byte[] fileBytes = file.getBytes();
            if (fileBytes.length < expected.length) {
                return false;
            }
            for (int i = 0; i < expected.length; i++) {
                if (fileBytes[i] != expected[i]) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @NoRepeatSubmit
    @Operation(summary = "注销账号")
    @DeleteMapping("/deactivate")
    public Result<Void> deactivate(@RequestAttribute("userId") Long userId,
                                   HttpServletRequest request) {
        userService.deactivateAccount(userId);
        // 注销后立即使当前 Token 失效
        String authorization = request.getHeader("Authorization");
        String refreshToken = request.getHeader("Refresh-Token");
        authService.logout(authorization, refreshToken);
        return Result.success();
    }

    @NoRepeatSubmit
    @Operation(summary = "更新通知偏好")
    @PutMapping("/notification-preference")
    public Result<Void> updateNotificationPreference(@RequestAttribute("userId") Long userId,
                                                      @Validated @RequestBody NotificationPreferenceDTO dto) {
        userService.updateNotificationPreference(userId, dto);
        return Result.success();
    }
}
