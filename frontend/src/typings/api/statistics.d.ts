declare namespace Api {
  namespace Statistics {
    /** Legacy point type — kept for backward compatibility */
    interface TrendPoint {
      date: string;
      value: number;
    }

    /** WeightTrendVO / BmiTrendVO — parallel arrays */
    interface TrendData {
      xAxis: string[];
      yAxis: number[];
    }

    /** CheckinTrendVO */
    interface CheckinTrendData {
      xAxis: string[];
      completeRate: number[];
      totalDays: number[];
    }

    /** ExerciseTrendVO */
    interface ExerciseTrendData {
      xAxis: string[];
      minutesPerDay: number[];
    }

    /** CalorieTrendVO */
    interface CalorieTrendData {
      xAxis: string[];
      dailyCalories: number[];
    }

    /** ProgressVO */
    interface ProgressData {
      totalCheckinRate: number;
      exerciseCompleteRate: number;
      dietCompleteRate: number;
      weightChange: number;
      targetProgressPercent: number;
      goal: string;
    }

    /** CalorieDeficitVO */
    interface CalorieDeficitData {
      xAxis: string[];
      consumed: number[];
      burned: number[];
      net: number[];
    }

    /** NutrientRatioVO / ExerciseDistributionVO — name/value parallel arrays */
    interface NameValueData {
      names: string[];
      values: number[];
    }

    /** Single day entry inside DietTrendComparisonVO */
    interface DailyCal {
      date: string;
      calories: number;
      dayLabel: string;
    }

    /** DietTrendComparisonVO */
    interface DietTrendComparisonData {
      currentTotalCalories: number;
      previousTotalCalories: number;
      calorieChangePercent: number;
      currentPeriodLabel: string;
      previousPeriodLabel: string;
      currentDaily: DailyCal[];
      previousDaily: DailyCal[];
    }
  }
}
