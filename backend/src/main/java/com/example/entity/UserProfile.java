package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("user_profile")
public class UserProfile implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 健康目标：LOSE_WEIGHT/GAIN_MUSCLE/STAY_HEALTHY/REHABILITATION/STRESS_RELIEF */
    private String healthGoal;

    /** 运动基础：SEDENTARY/OCCASIONAL/REGULAR/ADVANCED */
    private String fitnessLevel;

    /** 慢性疾病（逗号分隔） */
    private String chronicDiseases;

    /** 运动损伤（逗号分隔） */
    private String injuries;

    /** 饮食偏好/忌口（逗号分隔） */
    private String dietPreferences;

    /** 每日可用运动时间(分钟) */
    private Integer dailyAvailableMin;

    /** 睡眠质量自评：POOR/AVERAGE/GOOD */
    private String sleepQuality;

    /** 压力水平：LOW/MEDIUM/HIGH */
    private String stressLevel;

    /** 偏好语气：STRICT/COMFORTING/CELEBRATORY/NEUTRAL */
    private String preferredTone;

    /** 是否完成新手引导 */
    private Integer onboardingCompleted;

    /** 完成新手引导时间 */
    private LocalDateTime onboardingCompletedAt;

    /** 用户真实姓名 — 字段级加密 */
    @TableField(typeHandler = com.example.util.EncryptedStringTypeHandler.class)
    private String realName;

    /** 用户同意将数据用于模型训练 0=未授权 1=已授权 */
    private Integer dataConsentForModel;

    /** 用户同意将数据用于个性化推荐 0=未授权 1=已授权 */
    private Integer dataConsentForRecommend;

    /** 注册第几天 */
    private Integer registrationDay;

    /** 最后活跃时间 */
    private LocalDateTime lastActiveAt;

    /** 当前场景：workday / weekend / travel */
    private String currentScenario;

    /** 场景更新时间 */
    private LocalDateTime scenarioUpdatedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // --- getters/setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getHealthGoal() { return healthGoal; }
    public void setHealthGoal(String healthGoal) { this.healthGoal = healthGoal; }

    public String getFitnessLevel() { return fitnessLevel; }
    public void setFitnessLevel(String fitnessLevel) { this.fitnessLevel = fitnessLevel; }

    public String getChronicDiseases() { return chronicDiseases; }
    public void setChronicDiseases(String chronicDiseases) { this.chronicDiseases = chronicDiseases; }

    public String getInjuries() { return injuries; }
    public void setInjuries(String injuries) { this.injuries = injuries; }

    public String getDietPreferences() { return dietPreferences; }
    public void setDietPreferences(String dietPreferences) { this.dietPreferences = dietPreferences; }

    public Integer getDailyAvailableMin() { return dailyAvailableMin; }
    public void setDailyAvailableMin(Integer dailyAvailableMin) { this.dailyAvailableMin = dailyAvailableMin; }

    public String getSleepQuality() { return sleepQuality; }
    public void setSleepQuality(String sleepQuality) { this.sleepQuality = sleepQuality; }

    public String getStressLevel() { return stressLevel; }
    public void setStressLevel(String stressLevel) { this.stressLevel = stressLevel; }

    public String getPreferredTone() { return preferredTone; }
    public void setPreferredTone(String preferredTone) { this.preferredTone = preferredTone; }

    public Integer getOnboardingCompleted() { return onboardingCompleted; }
    public void setOnboardingCompleted(Integer onboardingCompleted) { this.onboardingCompleted = onboardingCompleted; }

    public LocalDateTime getOnboardingCompletedAt() { return onboardingCompletedAt; }
    public void setOnboardingCompletedAt(LocalDateTime onboardingCompletedAt) { this.onboardingCompletedAt = onboardingCompletedAt; }

    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }

    public Integer getDataConsentForModel() { return dataConsentForModel; }
    public void setDataConsentForModel(Integer dataConsentForModel) { this.dataConsentForModel = dataConsentForModel; }

    public Integer getDataConsentForRecommend() { return dataConsentForRecommend; }
    public void setDataConsentForRecommend(Integer dataConsentForRecommend) { this.dataConsentForRecommend = dataConsentForRecommend; }

    public Integer getRegistrationDay() { return registrationDay; }
    public void setRegistrationDay(Integer registrationDay) { this.registrationDay = registrationDay; }

    public LocalDateTime getLastActiveAt() { return lastActiveAt; }
    public void setLastActiveAt(LocalDateTime lastActiveAt) { this.lastActiveAt = lastActiveAt; }

    public String getCurrentScenario() { return currentScenario; }
    public void setCurrentScenario(String currentScenario) { this.currentScenario = currentScenario; }

    public LocalDateTime getScenarioUpdatedAt() { return scenarioUpdatedAt; }
    public void setScenarioUpdatedAt(LocalDateTime scenarioUpdatedAt) { this.scenarioUpdatedAt = scenarioUpdatedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}