package com.example.controller;

import com.example.common.Result;
import com.example.convert.UserConvert;
import com.example.dto.ResetPasswordDTO;
import com.example.dto.SendCodeDTO;
import com.example.dto.UserLoginByPhoneDTO;
import com.example.dto.UserLoginDTO;
import com.example.dto.UserRegisterDTO;
import com.example.service.AuthService;
import com.example.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证管理")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserConvert userConvert;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<LoginVO> register(@Validated @RequestBody UserRegisterDTO dto) {
        return Result.success(authService.register(dto));
    }

    @Operation(summary = "用户名密码登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Validated @RequestBody UserLoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    @Operation(summary = "手机号验证码登录")
    @PostMapping("/login-by-phone")
    public Result<LoginVO> loginByPhone(@Validated @RequestBody UserLoginByPhoneDTO dto) {
        return Result.success(authService.loginByPhone(dto));
    }

    @Operation(summary = "发送验证码")
    @PostMapping("/send-code")
    public Result<Void> sendCode(@Validated @RequestBody SendCodeDTO dto) {
        authService.sendCode(dto);
        return Result.success();
    }

    @Operation(summary = "忘记密码")
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Validated @RequestBody ResetPasswordDTO dto) {
        authService.resetPassword(dto);
        return Result.success();
    }

    @Operation(summary = "刷新Token")
    @PostMapping("/refresh")
    public Result<LoginVO> refresh(
            @Parameter(description = "刷新Token") @RequestHeader("Refresh-Token") String refreshToken) {
        return Result.success(authService.refresh(refreshToken));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout(
            @Parameter(description = "访问Token") @RequestHeader("Authorization") String authorization) {
        authService.logout(authorization);
        return Result.success();
    }
}
