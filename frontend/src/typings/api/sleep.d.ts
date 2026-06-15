declare namespace Api {
  namespace Sleep {
    interface SleepRecord {
      id: number;
      userId: number;
      sleepTime: string;
      wakeTime: string;
      duration: number;
      quality: string;
      note?: string;
      date: string;
      createdAt: string;
    }

    interface CreateSleepParams {
      sleepTime: string;
      wakeTime: string;
      quality: string;
      note?: string;
      date: string;
    }

    interface SleepStats {
      avgDuration: number;
      avgQuality: number;
      recentRecords: SleepRecord[];
    }
  }
}
