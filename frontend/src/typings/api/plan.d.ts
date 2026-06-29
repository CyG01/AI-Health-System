declare namespace Api {
  namespace Plan {
    /** Plan — aligned with backend AiPlanVO */
    interface Plan {
      id: number;
      planType: string;
      planName: string;
      durationDays: number;
      startDate: string;
      status: number;
      createTime: string;
      aiContent?: string;
    }

    /** Alias used by plan store */
    type AiPlan = Plan;

    interface GeneratePlanParams {
      planType: string;
      durationDays: number;
      intensity?: string;
      tastePreference?: string;
    }

    /** Plan detail item — aligned with backend AiPlanDetailVO.DetailItem */
    interface DetailItem {
      id: number;
      planId: number;
      daySequence: number;
      itemType: string;
      itemId: number;
      itemName: string;
      targetAmount: string;
      status: number;
    }

    /** Raw day structure from legacy/alternate API response format */
    interface RawPlanDay {
      day?: number;
      d?: number;
      tasks?: Array<{ description?: string; [key: string]: unknown }>;
      items?: unknown[];
    }

    /** Plan detail — aligned with backend AiPlanDetailVO */
    interface PlanDetail {
      id: number;
      planType: string;
      planName: string;
      durationDays: number;
      startDate: string;
      status: number;
      createTime: string;
      aiContent: string;
      details: DetailItem[];
      /** Legacy alternate format — some endpoints embed days directly */
      days?: RawPlanDay[];
    }

    /** Plan feedback — aligned with backend PlanFeedbackVO */
    interface PlanFeedback {
      id: number;
      planId: number;
      userId: number;
      feedbackType: string;
      content: string;
      satisfactionScore: number;
      adjustmentSuggestion: string;
      isAdjusted: number;
      newPlanId: number | null;
      createTime: string;
    }

    /** Adjust plan AI response — SDUI format with text/widgets or summary/changes */
    interface AdjustPlanResponse {
      text?: string;
      summary?: string;
      widgets?: Array<{
        type?: string;
        title?: string;
        description?: string;
        detail?: string;
        reason?: string;
      }>;
      changes?: Array<{
        type: string;
        description: string;
        reason: string;
      }>;
    }
  }
}
