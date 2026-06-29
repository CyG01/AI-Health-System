declare namespace Api {
  namespace Health {
    /** Allergy type enum */
    type AllergyType = 'FOOD' | 'DRUG' | 'ENVIRONMENT';

    /** Health record — aligned with backend HealthRecordVO */
    interface HealthRecord {
      id: number;
      height: number;
      weight: number;
      targetWeight: number;
      bmi: number;
      bmr: number;
      dailyCalorie: number;
      goal: string;
      diseaseHistory: string;
      allergyHistory: string;
      allergyType: AllergyType;
      familyHistory: string;
      medication: string;
      exerciseHabit: string;
      dietHabit: string;
      isLatest: number;
      createTime: string;
      /** Frontend-only field, not returned by backend */
      gender?: string;
    }

    interface CreateHealthParams {
      height: number;
      weight: number;
      targetWeight?: number;
      goal: string;
      diseaseHistory?: string;
      allergyHistory?: string;
      allergyType?: AllergyType;
      familyHistory?: string;
      medication?: string;
      exerciseHabit?: string;
      dietHabit?: string;
      /** Frontend-only field, may not be persisted by backend */
      gender?: string;
    }

    interface UpdateHealthParams extends CreateHealthParams {}

    /** Trend data point used in assessment */
    interface TrendPoint {
      date: string;
      value: number;
    }

    /** Health assessment — aligned with backend HealthAssessmentVO (extends HealthRecordVO) */
    interface HealthAssessment extends HealthRecord {
      bmiLevel: string;
      risks: string[];
      healthScore: number;
      aiSuggestion: string;
      weightTrend: TrendPoint[];
      bmiTrend: TrendPoint[];
      estimatedBodyFatRate: number;
      bodyFatLevel: string;
      bmrAssessment: string;
      cardiovascularRisk: string;
      exerciseAbility: string;
    }

    /** Health progress — aligned with backend HealthProgressVO */
    interface HealthProgress {
      currentWeight: number;
      targetWeight: number;
      initialWeight: number;
      lostWeight: number;
      remainingWeight: number;
      progressPercent: number;
      completed: boolean;
      weightTrend: TrendPoint[];
    }

    /** Health history record — aligned with backend HealthHistoryVO */
    interface HealthHistoryRecord {
      id: number;
      height?: number;
      weight?: number;
      bmi?: number;
      bmr?: number;
      dailyCalorie?: number;
      goal?: string;
      createTime?: string;
    }
  }
}
