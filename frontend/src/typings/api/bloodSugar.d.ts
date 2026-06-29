declare namespace Api {
  namespace BloodSugar {
    interface Record {
      id: number;
      userId: number;
      recordDate: string;
      recordTime?: string;
      measureType: string;
      glucoseValue: number;
      note?: string;
      abnormalFlag?: number;
      createTime?: string;
    }

    interface CreateParams {
      recordDate: string;
      recordTime?: string;
      measureType: string;
      glucoseValue: number;
      note?: string;
    }

    interface Stats {
      avg: number;
      max: number;
      min: number;
      records: Record[];
    }
  }
}
