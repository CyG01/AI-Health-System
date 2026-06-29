package com.example.vo;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 管理员用户详情
 */
public class AdminUserDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // 基本信息
    private Long id;
    private String username;
    private String phone;
    private String avatar;
    private String role;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime lastLoginTime;

    // 健康档案
    private Integer height;
    private Integer weight;
    private Integer age;
    private Integer gender;
    private String bmiLevel;

    // 计划统计
    private Integer totalPlans;
    private Integer activePlanCount;

    // 打卡统计
    private Integer totalCheckinDays;
    private Integer consecutiveDays;
    private LocalDate lastCheckinDate;

    // 运动统计
    private Integer totalExerciseRecords;
    private Integer totalExerciseCalories;

    // 饮食统计
    private Integer totalDietRecords;
    private Integer totalDietCalories;

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getLastLoginTime() { return lastLoginTime; }
    public void setLastLoginTime(LocalDateTime lastLoginTime) { this.lastLoginTime = lastLoginTime; }

    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }

    public Integer getWeight() { return weight; }
    public void setWeight(Integer weight) { this.weight = weight; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public Integer getGender() { return gender; }
    public void setGender(Integer gender) { this.gender = gender; }

    public String getBmiLevel() { return bmiLevel; }
    public void setBmiLevel(String bmiLevel) { this.bmiLevel = bmiLevel; }

    public Integer getTotalPlans() { return totalPlans; }
    public void setTotalPlans(Integer totalPlans) { this.totalPlans = totalPlans; }

    public Integer getActivePlanCount() { return activePlanCount; }
    public void setActivePlanCount(Integer activePlanCount) { this.activePlanCount = activePlanCount; }

    public Integer getTotalCheckinDays() { return totalCheckinDays; }
    public void setTotalCheckinDays(Integer totalCheckinDays) { this.totalCheckinDays = totalCheckinDays; }

    public Integer getConsecutiveDays() { return consecutiveDays; }
    public void setConsecutiveDays(Integer consecutiveDays) { this.consecutiveDays = consecutiveDays; }

    public LocalDate getLastCheckinDate() { return lastCheckinDate; }
    public void setLastCheckinDate(LocalDate lastCheckinDate) { this.lastCheckinDate = lastCheckinDate; }

    public Integer getTotalExerciseRecords() { return totalExerciseRecords; }
    public void setTotalExerciseRecords(Integer totalExerciseRecords) { this.totalExerciseRecords = totalExerciseRecords; }

    public Integer getTotalExerciseCalories() { return totalExerciseCalories; }
    public void setTotalExerciseCalories(Integer totalExerciseCalories) { this.totalExerciseCalories = totalExerciseCalories; }

    public Integer getTotalDietRecords() { return totalDietRecords; }
    public void setTotalDietRecords(Integer totalDietRecords) { this.totalDietRecords = totalDietRecords; }

    public Integer getTotalDietCalories() { return totalDietCalories; }
    public void setTotalDietCalories(Integer totalDietCalories) { this.totalDietCalories = totalDietCalories; }
}