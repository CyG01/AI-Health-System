package com.example.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;

import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("sys_user")
public class SysUser implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String phone;

    private String nickname;

    private String avatar;

    private Integer gender;

    private Integer age;

    /** 通知开关 1=开启 0=关闭 */
    private Integer notificationEnabled;

    /** 提醒时间 HH:mm */
    private String reminderTime;

    /** 运动提醒 1=开 0=关 */
    private Integer notifyExercise;

    /** 饮食提醒 1=开 0=关 */
    private Integer notifyDiet;

    /** 打卡提醒 1=开 0=关 */
    private Integer notifyCheckin;

    /** 安静时段开始 HH:mm */
    private String quietStart;

    /** 安静时段结束 HH:mm */
    private String quietEnd;

    private String role;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;

    @Version
    private Integer version;

    /** 免责声明接受时间（注册时记录） */
    private LocalDateTime disclaimerAcceptedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Integer getGender() {
        return gender;
    }

    public void setGender(Integer gender) {
        this.gender = gender;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Integer getNotificationEnabled() { return notificationEnabled; }
    public void setNotificationEnabled(Integer notificationEnabled) { this.notificationEnabled = notificationEnabled; }

    public String getReminderTime() { return reminderTime; }
    public void setReminderTime(String reminderTime) { this.reminderTime = reminderTime; }

    public Integer getNotifyExercise() { return notifyExercise; }
    public void setNotifyExercise(Integer notifyExercise) { this.notifyExercise = notifyExercise; }

    public Integer getNotifyDiet() { return notifyDiet; }
    public void setNotifyDiet(Integer notifyDiet) { this.notifyDiet = notifyDiet; }

    public Integer getNotifyCheckin() { return notifyCheckin; }
    public void setNotifyCheckin(Integer notifyCheckin) { this.notifyCheckin = notifyCheckin; }

    public String getQuietStart() { return quietStart; }
    public void setQuietStart(String quietStart) { this.quietStart = quietStart; }

    public String getQuietEnd() { return quietEnd; }
    public void setQuietEnd(String quietEnd) { this.quietEnd = quietEnd; }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public LocalDateTime getDisclaimerAcceptedAt() {
        return disclaimerAcceptedAt;
    }

    public void setDisclaimerAcceptedAt(LocalDateTime disclaimerAcceptedAt) {
        this.disclaimerAcceptedAt = disclaimerAcceptedAt;
    }
}
