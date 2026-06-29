declare namespace Api {
  namespace Exercise {
    /** Exercise record — aligned with backend ExerciseRecordVO */
    interface ExerciseRecord {
      id: number;
      userId: number;
      checkinId: number;
      itemId: number;
      itemName: string;
      durationMinutes: number;
      caloriesBurned: number;
      createTime: string;
    }

    interface CreateExerciseParams {
      itemId?: number;
      itemName?: string;
      durationMinutes: number;
      caloriesBurned?: number;
      checkinId?: number;
    }

    /** Exercise item — aligned with backend ExerciseItemVO */
    interface ExerciseItem {
      id: number;
      name: string;
      type: string;
      calorieCoefficient: number;
      targetMuscle: string;
      difficulty: string;
      videoUrl: string;
      status: number;
    }
  }
}
