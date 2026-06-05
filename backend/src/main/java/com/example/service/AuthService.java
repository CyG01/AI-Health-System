package com.example.service;

import com.example.dto.ResetPasswordDTO;
import com.example.dto.SendCodeDTO;
import com.example.dto.UserLoginByPhoneDTO;
import com.example.dto.UserLoginDTO;
import com.example.dto.UserRegisterDTO;
import com.example.vo.LoginVO;

public interface AuthService {

    LoginVO register(UserRegisterDTO dto);

    LoginVO login(UserLoginDTO dto);

    LoginVO loginByPhone(UserLoginByPhoneDTO dto);

    void sendCode(SendCodeDTO dto);

    void resetPassword(ResetPasswordDTO dto);

    LoginVO refresh(String refreshToken);

    void logout(String authorization);
}
