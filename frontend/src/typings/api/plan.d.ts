declare namespace Api {
  namespace Plan {
    interface Plan {
      id: number;
      userId: number;
      planType: string;
      planName: string;
      title: string;
      content: string;
      aiContent?: string;
      durationDays: number;
      intensity?: string;
      tastePreference?: string;
      status: number;
      startDate: string;
      createdAt: string;
      createTime?: string;
      updatedAt: string;
      completedAt?: string;
      days?: PlanDay[];
    }

    /** Alias used by plan store */
    type AiPlan = Plan;

    interface GeneratePlanParams {
      planType: string;
      durationDays: number;
      intensity?: string;
      tastePreference?: string;
    }

    interface PlanDetail extends Plan {
      days: PlanDay[];
      recommendations?: string;
    }

    interface PlanDay {
      d?: number;
      day?: number;
      title?: string;
      items?: (string | any)[];
      tasks?: PlanTask[];
    }

    interface PlanTask {
      id: number;
      type: string;
      description: string;
      duration?: string;
      completed: boolean;
    }

    interface PlanFeedback {
      id: number;
      planId: number;
      userId: number;
      rating: number;
      content: string;
      createdAt: string;
    }
  }
}
