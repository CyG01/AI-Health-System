declare namespace Api {
  namespace Checkin {
    interface CheckinRecord {
      id: number;
      userId: number;
      planId?: number;
      checkDate: string;
      exerciseStatus: number;
      dietStatus: number;
      currentWeight?: number;
      mood?: string;
      note?: string;
      isSupplement?: number;
      createTime?: string;
    }

    interface CheckinStats {
      consecutiveDays: number;
      totalDays: number;
      currentWeekDays: number;
      currentMonthDays: number;
      exerciseCompleteRate: number;
      dietCompleteRate: number;
    }

    interface CalendarData {
      consecutiveDays: number;
      totalDays: number;
      currentWeekDays: number;
      currentMonthDays: number;
      exerciseCompleteRate: number;
      dietCompleteRate: number;
    }

    interface CheckinParams {
      planId?: number;
      exerciseStatus?: number;
      dietStatus?: number;
      currentWeight?: number;
      mood?: string;
      note?: string;
    }
  }
}
