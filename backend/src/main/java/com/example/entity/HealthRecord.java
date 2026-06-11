package com.example.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("health_record")
public class HealthRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Integer height;

    private Integer weight;

    private Integer targetWeight;

    private BigDecimal bmi;

    private Integer bmr;

    private Integer dailyCalorie;

    private String goal;

    private String diseaseHistory;

    private String allergyHistory;

    /** 过敏类型（逗号分隔）：FOOD/DRUG/ENVIRONMENT */
    private String allergyType;

    /** 家族病史 */
    private String familyHistory;

    /** 当前用药情况 */
    private String medication;

    private String exerciseHabit;

    private String dietHabit;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer isDeleted;

    @Version
    private Integer version;

    private Integer isLatest;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }

    public Integer getWeight() { return weight; }
    public void setWeight(Integer weight) { this.weight = weight; }

    public Integer getTargetWeight() { return targetWeight; }
    public void setTargetWeight(Integer targetWeight) { this.targetWeight = targetWeight; }

    public BigDecimal getBmi() { return bmi; }
    public void setBmi(BigDecimal bmi) { this.bmi = bmi; }

    public Integer getBmr() { return bmr; }
    public void setBmr(Integer bmr) { this.bmr = bmr; }

    public Integer getDailyCalorie() { return dailyCalorie; }
    public void setDailyCalorie(Integer dailyCalorie) { this.dailyCalorie = dailyCalorie; }

    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }

    public String getDiseaseHistory() { return diseaseHistory; }
    public void setDiseaseHistory(String diseaseHistory) { this.diseaseHistory = diseaseHistory; }

    public String getAllergyHistory() { return allergyHistory; }
    public void setAllergyHistory(String allergyHistory) { this.allergyHistory = allergyHistory; }

    public String getAllergyType() { return allergyType; }
    public void setAllergyType(String allergyType) { this.allergyType = allergyType; }

    public String getFamilyHistory() { return familyHistory; }
    public void setFamilyHistory(String familyHistory) { this.familyHistory = familyHistory; }

    public String getMedication() { return medication; }
    public void setMedication(String medication) { this.medication = medication; }

    public String getExerciseHabit() { return exerciseHabit; }
    public void setExerciseHabit(String exerciseHabit) { this.exerciseHabit = exerciseHabit; }

    public String getDietHabit() { return dietHabit; }
    public void setDietHabit(String dietHabit) { this.dietHabit = dietHabit; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public Integer getIsLatest() { return isLatest; }
    public void setIsLatest(Integer isLatest) { this.isLatest = isLatest; }
}