package com.example.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.vo.UserInfoVO;

public interface AdminUserService {

    Page<UserInfoVO> listUsers(int page, int size, String keyword);

    void banUser(Long userId);

    void unbanUser(Long userId);
}
