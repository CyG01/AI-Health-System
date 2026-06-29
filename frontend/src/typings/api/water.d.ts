declare namespace Api {
  namespace Water {
    /** Matches backend WaterRecordVO */
    interface WaterRecord {
      id: number;
      recordDate: string;
      amountMl: number;
      createTime: string;
    }

    /** Matches backend WaterRecordSubmitDTO */
    interface CreateWaterParams {
      amountMl: number;
      recordDate: string;
    }

    interface DailyWaterSummary {
      id: number;
      recordDate: string;
      amountMl: number;
      createTime: string;
    }

  }
}
