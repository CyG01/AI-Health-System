declare namespace Api {
  namespace Food {
    /** Food record — aligned with backend DietRecordVO */
    interface FoodRecord {
      id: number;
      userId: number;
      checkinId: number;
      mealType: string;
      itemId: number;
      itemName: string;
      weightGrams: number;
      caloriesConsumed: number;
      protein: number;
      fat: number;
      carbs: number;
      foodName: string;
      category: string;
      note: string;
      remark: string;
      createTime: string;
    }

    interface CreateFoodParams {
      mealType: string;
      itemId?: number;
      foodName?: string;
      weightGrams: number;
      caloriesConsumed?: number;
      protein?: number;
      carbs?: number;
      fat?: number;
      note?: string;
      remark?: string;
      checkinId?: number;
      category?: string;
    }

    /** Food item — aligned with backend FoodItemVO (per 100g values) */
    interface FoodItem {
      id: number;
      name: string;
      category: string;
      caloriePer100g: number;
      proteinPer100g: number;
      carbsPer100g: number;
      fatPer100g: number;
      imageUrl: string;
      foodSource: string;
      sort: number;
      status: number;
    }
  }
}
