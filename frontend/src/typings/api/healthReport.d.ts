declare namespace Api {
  namespace HealthReport {
    /** Matches backend HealthReportVO */
    interface HealthReportVO {
      id: number;
      reportType: string;
      reportPeriod: string;
      aiContent: string;
      createTime: string;
      isRead: number;
    }
  }
}
