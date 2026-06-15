declare namespace Api {
  namespace Health {
    /** Allergy type enum */
    type AllergyType = 'FOOD' | 'DRUG' | 'ENVIRONMENT';

    interface HealthRecord {
      id: number;
      userId: number;
      height: number;
      weight: number;
      gender: string;
      targetWeight: number;
      bmi: number;
      bmr: number;
      goal: string;
      diseaseHistory: string;
      allergyHistory: string;
      allergyType: AllergyType;
      familyHistory: string;
      medication: string;
      exerciseHabit: string;
      dietHabit: string;
      createdAt: string;
      updatedAt: string;
    }

    interface CreateHealthParams {
      height: number;
      weight: number;
      gender?: string;
      targetWeight?: number;
      goal: string;
      diseaseHistory?: string;
      allergyHistory?: string;
      allergyType?: AllergyType;
      familyHistory?: string;
      medication?: string;
      exerciseHabit?: string;
      dietHabit?: string;
    }

    interface UpdateHealthParams extends CreateHealthParams {}

    interface HealthAssessment {
      score: number;
      level: string;
      suggestions: string[];
    }

    interface HealthProgress {
      weightChange: number;
      bmiChange: number;
      recordsCount: number;
      streakDays: number;
    }

    /** Health history record - matches backend HealthHistoryVO */
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
