declare namespace Api {
  namespace Food {
    interface FoodRecord {
      id: number;
      userId: number;
      mealType: string;
      foodName: string;
      quantity: number;
      unit: string;
      calories: number;
      protein?: number;
      carbs?: number;
      fat?: number;
      fiber?: number;
      mealTime?: string;
      date: string;
      createdAt: string;
    }

    interface CreateFoodParams {
      mealType: string;
      foodName: string;
      quantity: number;
      unit: string;
      calories: number;
      protein?: number;
      carbs?: number;
      fat?: number;
      fiber?: number;
      mealTime?: string;
      date: string;
    }

    interface FoodItem {
      id: number;
      name: string;
      calories: number;
      protein: number;
      carbs: number;
      fat: number;
      unit: string;
    }

    interface DailyFoodSummary {
      totalCalories: number;
      totalProtein: number;
      totalCarbs: number;
      totalFat: number;
      records: FoodRecord[];
    }
  }
}
