declare namespace Api {
  namespace Exercise {
    interface ExerciseRecord {
      id: number;
      userId: number;
      exerciseType: string;
      exerciseName: string;
      duration: number;
      caloriesBurned: number;
      intensity?: string;
      note?: string;
      date: string;
      createdAt: string;
    }

    interface CreateExerciseParams {
      exerciseType: string;
      exerciseName: string;
      duration: number;
      caloriesBurned: number;
      intensity?: string;
      note?: string;
      date: string;
    }

    interface ExerciseItem {
      id: number;
      name: string;
      type: string;
      caloriesPerMinute: number;
    }

    interface DailyExerciseSummary {
      totalCaloriesBurned: number;
      totalDuration: number;
      records: ExerciseRecord[];
    }
  }
}
