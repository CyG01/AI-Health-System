package com.example.sdui;

/**
 * 饮食图表组件 — 展示热量和营养素的摄入与剩余情况。
 */
public class MealChartWidget extends Widget {

    private Integer totalCalories;
    private Integer protein;
    private Integer carbs;
    private Integer fat;
    private Integer remainingCalories;
    private String mealSuggestion;

    public MealChartWidget() {
        this.type = "meal_chart";
    }

    public Integer getTotalCalories() {
        return totalCalories;
    }

    public void setTotalCalories(Integer totalCalories) {
        this.totalCalories = totalCalories;
    }

    public Integer getProtein() {
        return protein;
    }

    public void setProtein(Integer protein) {
        this.protein = protein;
    }

    public Integer getCarbs() {
        return carbs;
    }

    public void setCarbs(Integer carbs) {
        this.carbs = carbs;
    }

    public Integer getFat() {
        return fat;
    }

    public void setFat(Integer fat) {
        this.fat = fat;
    }

    public Integer getRemainingCalories() {
        return remainingCalories;
    }

    public void setRemainingCalories(Integer remainingCalories) {
        this.remainingCalories = remainingCalories;
    }

    public String getMealSuggestion() {
        return mealSuggestion;
    }

    public void setMealSuggestion(String mealSuggestion) {
        this.mealSuggestion = mealSuggestion;
    }
}