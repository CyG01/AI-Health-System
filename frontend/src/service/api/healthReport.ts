import { request } from '../request';

/** Generate health report — backend returns HealthReportVO */
export function fetchGenerateReport(reportType: string = 'weekly') {
  return request<Api.HealthReport.HealthReportVO>({
    url: '/health-report/generate',
    method: 'post',
    data: { reportType }
  });
}

/** Get report list */
export function fetchGetReportList(
  page: number = 1,
  size: number = 10
) {
  return request<Api.HealthReport.HealthReportVO[]>({
    url: '/health-report/list',
    method: 'get',
    params: { page, size }
  });
}

/** Get report detail — backend returns HealthReportVO (auto marks as read) */
export function fetchGetReportDetail(reportId: number) {
  return request<Api.HealthReport.HealthReportVO>({
    url: `/health-report/${reportId}`,
    method: 'get'
  });
}
