declare namespace Api {
  namespace Health {
    interface BodyMeasurement {
      id: number;
      chest: number | null;
      waist: number | null;
      hip: number | null;
      arm: number | null;
      thigh: number | null;
      bodyFatRate: number | null;
      waistHipRatio: number | null;
      recordDate: string;
      createTime: string;
      note: string;
    }

    /** Submit DTO — excludes server-computed fields (id, waistHipRatio, createTime) */
    interface BodyMeasurementSubmitRequest {
      recordDate: string;
      waist?: number | null;
      hip?: number | null;
      chest?: number | null;
      thigh?: number | null;
      arm?: number | null;
      bodyFatRate?: number | null;
      note?: string;
    }

    interface BodyMeasurementTrendPoint {
      field: string;
      trend: TrendPoint[];
    }

    interface TrendPoint {
      date: string;
      value: number;
    }
  }
}
