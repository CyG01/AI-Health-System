package com.example.event;

import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;

/**
 * 食物识别完成事件。
 * FoodRecognitionServiceImpl 识别完成后发布此事件。
 */
public class FoodRecognizedEvent extends ApplicationEvent {

    private final Long userId;
    private final String foodName;
    private final int calories;
    private final int protein;
    private final int carbs;
    private final int fat;
    private final String category;
    private final int recommendedGrams;
    private final LocalDate recognizedAt;

    public FoodRecognizedEvent(Object source, Long userId, String foodName,
                               int calories, int protein, int carbs, int fat,
                               String category, int recommendedGrams) {
        super(source);
        this.userId = userId;
        this.foodName = foodName;
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fat = fat;
        this.category = category;
        this.recommendedGrams = recommendedGrams;
        this.recognizedAt = LocalDate.now();
    }

    public Long getUserId() {
        return userId;
    }

    public String getFoodName() {
        return foodName;
    }

    public int getCalories() {
        return calories;
    }

    public int getProtein() {
        return protein;
    }

    public int getCarbs() {
        return carbs;
    }

    public int getFat() {
        return fat;
    }

    public String getCategory() {
        return category;
    }

    public int getRecommendedGrams() {
        return recommendedGrams;
    }

    public LocalDate getRecognizedAt() {
        return recognizedAt;
    }
}