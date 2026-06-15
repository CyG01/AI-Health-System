declare namespace Api {
  namespace Goal {
    interface GoalItem {
      id: number;
      userId: number;
      title: string;
      description: string;
      targetDate: string;
      completedDate?: string;
      status: string;
      progress: number;
      createdAt: string;
    }

    interface GoalCreateRequest {
      title: string;
      description?: string;
      targetDate: string;
    }

    interface GoalUpdateRequest {
      title?: string;
      description?: string;
      targetDate?: string;
    }
  }
}
