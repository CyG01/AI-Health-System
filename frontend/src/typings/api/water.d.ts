declare namespace Api {
  namespace Water {
    interface WaterRecord {
      id: number;
      userId: number;
      amount: number;
      unit: string;
      time: string;
      date: string;
      createdAt: string;
    }

    interface CreateWaterParams {
      amount: number;
      unit?: string;
      time?: string;
      date: string;
    }

    interface DailyWaterSummary {
      totalAmount: number;
      goal: number;
      records: WaterRecord[];
    }

    /** Response from GET /water/total?date= */
    interface WaterDailyTotal {
      date: string;
      totalMl: number;
    }
  }
}
