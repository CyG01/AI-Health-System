declare namespace Api {
  namespace Statistics {
    interface DashboardData {
      calories: CalorieStats;
      exercise: ExerciseStats;
      sleep: SleepStats;
      weight: WeightStats;
      nutrition: NutritionStats;
    }

    interface CalorieStats {
      consumed: number;
      burned: number;
      net: number;
      trend: TrendPoint[];
    }

    interface ExerciseStats {
      totalDuration: number;
      totalCalories: number;
      sessions: number;
      trend: TrendPoint[];
    }

    interface SleepStats {
      avgDuration: number;
      avgQuality: number;
      trend: TrendPoint[];
    }

    interface WeightStats {
      current: number;
      change: number;
      trend: TrendPoint[];
    }

    interface NutritionStats {
      protein: number;
      carbs: number;
      fat: number;
      fiber: number;
    }

    interface TrendPoint {
      date: string;
      value: number;
    }
  }
}
