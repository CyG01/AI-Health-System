package com.example.controller;

import com.example.common.Result;
import com.example.convert.UserConvert;
import com.example.dto.UpdatePasswordDTO;
import com.example.dto.UpdateProfileDTO;
import com.example.service.UserService;
import com.example.vo.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "个人中心")
@RestController
@RequestMapping("/api/auth")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserConvert userConvert;

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

    @Operation(summary = "更新个人信息")
    @PutMapping("/update-profile")
    public Result<UserInfoVO> updateProfile(@Validated @RequestBody UpdateProfileDTO dto,
                                            @RequestAttribute("userId") Long userId) {
        return Result.success(userService.updateProfile(userId, dto));
    }
}
