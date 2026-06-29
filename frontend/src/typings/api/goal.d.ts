declare namespace Api {
  namespace Goal {
    interface GoalItem {
      id: number;
      goalType: string;
      goalTypeLabel: string;
      goalName: string;
      targetValue: number;
      currentValue: number;
      unit: string;
      progressPercent: number;
      startDate: string;
      targetDate: string;
      remainingDays: number;
      status: number;
      statusLabel: string;
      completedDate: string;
      createTime: string;
    }

    interface GoalCreateRequest {
      goalType: string;
      goalName: string;
      targetValue: number;
      unit?: string;
      startDate?: string;
      targetDate?: string;
    }

    interface GoalUpdateRequest {
      id: number;
      goalType: string;
      goalName: string;
      targetValue: number;
      unit?: string;
      startDate?: string;
      targetDate?: string;
    }
  }
}
