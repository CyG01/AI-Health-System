declare namespace Api {
  namespace Sleep {
    interface SleepRecord {
      id: number;
      userId?: number;
      recordDate: string;
      sleepTime: string;
      wakeTime: string;
      durationMin: number;
      duration?: number;
      quality: number | string;
      note?: string;
      dreamNotes?: string;
      date?: string;
      createdAt?: string;
    }

    interface CreateSleepParams {
      recordDate?: string;
      sleepTime: string;
      wakeTime: string;
      quality: number | string;
      note?: string;
      dreamNotes?: string;
      date?: string;
    }

    interface SleepStats {
      avgDuration: number;
      avgQuality: number;
      recentRecords: SleepRecord[];
      analysis?: string;
    }
  }
}
