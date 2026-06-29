package com.example.sdui;

import java.util.ArrayList;
import java.util.List;

/**
 * 餐饮计划组件 — 展示一日三餐推荐和营养素分配。
 */
public class MealPlanWidget extends Widget {

    private String mealType;
    private Integer totalCalories;
    private List<MealItem> items = new ArrayList<>();
    private NutritionSummary nutrition;
    private String cookingTip;

    public MealPlanWidget() {
        this.type = "meal_plan";
    }

    public String getMealType() { return mealType; }
    public void setMealType(String mealType) { this.mealType = mealType; }

    public Integer getTotalCalories() { return totalCalories; }
    public void setTotalCalories(Integer totalCalories) { this.totalCalories = totalCalories; }

    public List<MealItem> getItems() { return items; }
    public void setItems(List<MealItem> items) { this.items = items; }

    public NutritionSummary getNutrition() { return nutrition; }
    public void setNutrition(NutritionSummary nutrition) { this.nutrition = nutrition; }

    public String getCookingTip() { return cookingTip; }
    public void setCookingTip(String cookingTip) { this.cookingTip = cookingTip; }

    /**
     * 餐食条目。
     */
    public static class MealItem {
        private String name;
        private Integer calories;
        private Integer protein;
        private String imageUrl;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public Integer getCalories() { return calories; }
        public void setCalories(Integer calories) { this.calories = calories; }

        public Integer getProtein() { return protein; }
        public void setProtein(Integer protein) { this.protein = protein; }

        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    }

    /**
     * 营养素汇总。
     */
    public static class NutritionSummary {
        private Integer protein;
        private Integer carbs;
        private Integer fat;
        private Integer fiber;

        public Integer getProtein() { return protein; }
        public void setProtein(Integer protein) { this.protein = protein; }

        public Integer getCarbs() { return carbs; }
        public void setCarbs(Integer carbs) { this.carbs = carbs; }

        public Integer getFat() { return fat; }
        public void setFat(Integer fat) { this.fat = fat; }

        public Integer getFiber() { return fiber; }
        public void setFiber(Integer fiber) { this.fiber = fiber; }
    }
}