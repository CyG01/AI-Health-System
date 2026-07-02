package com.example.controller;

import com.example.annotation.NoRepeatSubmit;
import com.example.annotation.RateLimit;
import com.example.common.Result;
import com.example.convert.UserConvert;
import com.example.dto.ResetPasswordDTO;
import com.example.dto.SendCodeDTO;
import com.example.dto.UserLoginByPhoneDTO;
import com.example.dto.UserLoginDTO;
import com.example.dto.UserRegisterDTO;
import com.example.service.AuthService;
import com.example.vo.CaptchaVO;
import com.example.vo.LoginVO;
import com.wf.captcha.SpecCaptcha;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Tag(name = "认证管理")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String CAPTCHA_PREFIX = "captcha:";
    private static final long CAPTCHA_EXPIRE_MINUTES = 2;

    // Fix for easy-captcha compatibility with Java 17+
    static {
        System.setProperty("java.awt.headless", "false");
    }

    private final AuthService authService;

    private final UserConvert userConvert;

    private final StringRedisTemplate stringRedisTemplate;

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Operation(summary = "获取验证码")
    @GetMapping("/captcha")
    public Result<CaptchaVO> captcha() {
        String uuid = UUID.randomUUID().toString();
        // Use simple SVG captcha for Java 22 compatibility (SpecCaptcha has AWT issues)
        return generateFallbackCaptcha(uuid);
    }

    private Result<CaptchaVO> generateFallbackCaptcha(String uuid) {
        try {
            // Generate a simple 4-digit numeric captcha
            String captchaText = String.valueOf((int) ((Math.random() * 9 + 1) * 1000));
            stringRedisTemplate.opsForValue().set(
                    CAPTCHA_PREFIX + uuid,
                    captchaText,
                    CAPTCHA_EXPIRE_MINUTES,
                    TimeUnit.MINUTES);

            // Create a simple SVG captcha with noise lines
            StringBuilder svg = new StringBuilder();
            svg.append("<svg xmlns='http://www.w3.org/2000/svg' width='130' height='48'>");
            svg.append("<rect width='130' height='48' fill='#f5f5f5'/>");
            // Add noise lines
            for (int i = 0; i < 5; i++) {
                int x1 = (int) (Math.random() * 130);
                int y1 = (int) (Math.random() * 48);
                int x2 = (int) (Math.random() * 130);
                int y2 = (int) (Math.random() * 48);
                svg.append("<line x1='").append(x1).append("' y1='").append(y1)
                   .append("' x2='").append(x2).append("' y2='").append(y2)
                   .append("' stroke='#ccc' stroke-width='1'/>");
            }
            // Add captcha text
            svg.append("<text x='65' y='34' font-size='26' font-family='Arial, sans-serif' fill='#2c3e50' text-anchor='middle' font-weight='bold'>");
            svg.append(captchaText);
            svg.append("</text></svg>");

            String base64 = "data:image/svg+xml;base64," + java.util.Base64.getEncoder().encodeToString(svg.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));

            CaptchaVO captchaVO = new CaptchaVO();
            captchaVO.setBase64(base64);
            captchaVO.setUuid(uuid);
            return Result.success(captchaVO);
        } catch (Exception ex) {
            log.error("验证码生成失败", ex);
            return Result.fail("验证码生成失败，请稍后重试");
        }
    }

    @RateLimit(time = 60, count = 3)
    @NoRepeatSubmit
    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<LoginVO> register(@Validated @RequestBody UserRegisterDTO dto) {
        return Result.success(authService.register(dto));
    }

    @RateLimit(time = 60, count = 5)
    @NoRepeatSubmit
    @Operation(summary = "用户名密码登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Validated @RequestBody UserLoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    @RateLimit(time = 60, count = 3)
    @NoRepeatSubmit
    @Operation(summary = "手机号验证码登录")
    @PostMapping("/login-by-phone")
    public Result<LoginVO> loginByPhone(@Validated @RequestBody UserLoginByPhoneDTO dto) {
        return Result.success(authService.loginByPhone(dto));
    }

    @RateLimit(time = 60, count = 1)
    @NoRepeatSubmit
    @Operation(summary = "发送验证码")
    @PostMapping("/send-code")
    public Result<Void> sendCode(@Validated @RequestBody SendCodeDTO dto) {
        authService.sendCode(dto);
        return Result.success();
    }

    @RateLimit(time = 60, count = 3)
    @NoRepeatSubmit
    @Operation(summary = "忘记密码")
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Validated @RequestBody ResetPasswordDTO dto) {
        authService.resetPassword(dto);
        return Result.success();
    }

    @NoRepeatSubmit
    @Operation(summary = "刷新Token")
    @PostMapping("/refresh")
    public Result<LoginVO> refresh(
            @Parameter(description = "Refresh Token") @RequestHeader("Refresh-Token") String refreshToken) {
        return Result.success(authService.refresh(refreshToken));
    }

    @NoRepeatSubmit
    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout(
            @Parameter(description = "访问Token") @RequestHeader("Authorization") String authorization,
            @Parameter(description = "刷新Token") @RequestHeader(value = "Refresh-Token", required = false) String refreshToken) {
        authService.logout(authorization, refreshToken);
        return Result.success();
    }
}
