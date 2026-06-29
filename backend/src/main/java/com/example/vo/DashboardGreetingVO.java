package com.example.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 预测性问候卡片 VO。
 * 由 GreetingRuleEngine（规则引擎）生成，不消耗大模型 Token。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardGreetingVO {

    /** 卡片类型：morning / noon / afternoon / reminder / celebration */
    private String type;

    /** 图标 emoji */
    private String icon;

    /** 问候语：早安！/ 午饭时间到！等 */
    private String greeting;

    /** 主消息 */
    private String message;

    /** 详细描述 */
    private String detail;

    /** CTA 操作按钮 */
    private List<CardAction> actions;

    /** 今日计划完成进度（0-100），null 表示不显示进度条 */
    private Integer progress;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardAction {
        /** 按钮文字 */
        private String label;
        /** 路由地址 */
        private String url;
        /** 是否为主按钮 */
        private boolean primary;
        /** 特殊动作（如 open_copilot） */
        private String action;
    }
}
