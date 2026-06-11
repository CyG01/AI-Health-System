import request from '@/utils/request'

export function generateReport(reportType = 'weekly') {
  return request({ url: '/health-report/generate', method: 'post', data: { reportType } })
}

export function getReportList(page = 1, size = 10) {
  return request({ url: '/health-report/list', method: 'get', params: { page, size } })
}

export function getReportDetail(reportId) {
  return request({ url: `/health-report/${reportId}`, method: 'get' })
}