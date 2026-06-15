declare namespace Api {
  namespace Health {
    interface BodyMeasurement {
      id: number;
      userId: number;
      weight?: number;
      height?: number;
      chest?: number;
      waist?: number;
      hip?: number;
      arm?: number;
      thigh?: number;
      bodyFat?: number;
      muscle?: number;
      date: string;
      createdAt: string;
    }

    interface BodyMeasurementSubmitRequest extends Omit<BodyMeasurement, 'id' | 'userId' | 'createdAt'> {}

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
