package com.example.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 自然语言食物识别结果 VO（一句话记账）。
 * 返回 AI 解析后的食物列表，前端可调整克数后一键录入。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodTextRecognizeVO {

    /** 解析出的食物列表 */
    private List<FoodItem> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FoodItem {
        /** 食物名称 */
        private String foodName;
        /** 估算重量（克） */
        private Integer weightG;
        /** 估算热量（kcal） */
        private Integer calories;
    }
}
