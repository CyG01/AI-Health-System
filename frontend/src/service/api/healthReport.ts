import { request } from '../request';

/** Generate health report */
export function fetchGenerateReport(reportType: string = 'weekly') {
  return request<{ id: number; userId: number; reportType: string; content: string; score: number; suggestions: string[]; generatedAt: string }>({
    url: '/health-report/generate',
    method: 'post',
    data: { reportType }
  });
}

/** Get report list with optional filters */
export function fetchGetReportList(
  page: number = 1,
  size: number = 10,
  filters?: { reportType?: string; startDate?: string; endDate?: string }
) {
  return request<Api.Common.PageResult<{ id: number; userId: number; reportType: string; reportPeriod: string; content: string; aiContent: string; score: number; suggestions: string[]; isRead: number; generatedAt: string; createTime: string }>>({
    url: '/health-report/list',
    method: 'get',
    params: { page, size, ...filters }
  });
}

/** Get report detail */
export function fetchGetReportDetail(reportId: number) {
  return request<{ id: number; userId: number; reportType: string; content: string; score: number; suggestions: string[]; generatedAt: string }>({
    url: `/health-report/${reportId}`,
    method: 'get'
  });
}
