package com.example.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.vo.AdminUserDetailVO;
import com.example.vo.UserInfoVO;

import java.util.List;

public interface AdminUserService {

    Page<UserInfoVO> listUsers(int page, int size, String keyword, Integer status, String startDate, String endDate);

    AdminUserDetailVO getUserDetail(Long userId);

    void banUser(Long userId);

    void unbanUser(Long userId);

    List<UserInfoVO> exportUsers(String keyword, Integer status, String startDate, String endDate);
}
