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
import org.springframework.beans.factory.annotation.Autowired;
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
public class AuthController {

    private static final String CAPTCHA_PREFIX = "captcha:";
    private static final long CAPTCHA_EXPIRE_MINUTES = 2;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserConvert userConvert;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Operation(summary = "获取验证码")
    @GetMapping("/captcha")
    public Result<CaptchaVO> captcha() {
        SpecCaptcha captcha = new SpecCaptcha(120, 40, 4);
        String captchaText = captcha.text();
        String uuid = UUID.randomUUID().toString();
        stringRedisTemplate.opsForValue().set(
                CAPTCHA_PREFIX + uuid,
                captchaText,
                CAPTCHA_EXPIRE_MINUTES,
                TimeUnit.MINUTES);
        CaptchaVO captchaVO = new CaptchaVO();
        captchaVO.setBase64(captcha.toBase64());
        captchaVO.setUuid(uuid);
        return Result.success(captchaVO);
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
            @Parameter(description = "访问Token") @RequestHeader("Authorization") String authorization) {
        authService.logout(authorization);
        return Result.success();
    }
}
