package com.example.service;

import com.example.dto.UpdatePasswordDTO;
import com.example.dto.UpdateProfileDTO;
import com.example.vo.UserInfoVO;

public interface UserService {

    void updatePassword(Long userId, UpdatePasswordDTO dto);

    UserInfoVO getProfile(Long userId);

    UserInfoVO updateProfile(Long userId, UpdateProfileDTO dto);
}
