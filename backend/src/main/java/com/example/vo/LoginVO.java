package com.example.vo;

import java.io.Serializable;

public class LoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String accessToken;

    private String refreshToken;

    private UserInfoVO userInfo;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public UserInfoVO getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserInfoVO userInfo) {
        this.userInfo = userInfo;
    }
}
