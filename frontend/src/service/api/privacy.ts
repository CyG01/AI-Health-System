import { request } from '../request';

/** Get current user's data consent status */
export function fetchGetPrivacyConsent() {
  return request<Api.Privacy.ConsentStatus>({
    url: '/v1/privacy/consent',
    method: 'get'
  });
}

/** Update user's data consent preferences */
export function fetchUpdatePrivacyConsent(data: Api.Privacy.ConsentUpdateParams) {
  return request<Api.Privacy.ConsentStatus>({
    url: '/v1/privacy/consent',
    method: 'put',
    data
  });
}

/** Get AI memory sandbox status */
export function fetchGetMemorySandbox() {
  return request<{ enabled: boolean }>({
    url: '/v1/privacy/memory-sandbox',
    method: 'get'
  });
}

/** Toggle AI memory sandbox */
export function fetchToggleMemorySandbox(enabled: boolean) {
  return request<void>({
    url: '/v1/privacy/memory-sandbox',
    method: 'put',
    params: { enabled }
  });
}

/** Submit data purge request */
export function fetchDataPurge(data: { dataTypes: string[]; reason?: string }) {
  return request<{ userId: number; dataTypes: string[]; status: string; message: string; estimatedTime: string }>({
    url: '/v1/privacy/purge',
    method: 'post',
    data
  });
}

/** Get privacy statistics */
export function fetchPrivacyStatistics() {
  return request<Record<string, unknown>>({
    url: '/v1/privacy/statistics',
    method: 'get'
  });
}

/** Get privacy audit logs */
export function fetchPrivacyAuditLogs(limit = 20) {
  return request<Array<{ id: number; actionType: string; actionDescription: string; result: string; ipAddress: string; userAgent: string; createTime: string }>>({
    url: '/v1/privacy/audit-logs',
    method: 'get',
    params: { limit }
  });
}

/** Request data export */
export function fetchDataExport(data: { exportType: string; exportScope?: string }) {
  return request<{ taskId: string; status: string }>({
    url: '/v1/privacy/export',
    method: 'post',
    data
  });
}

/** Get export task status */
export function fetchDataExportStatus(taskId: string) {
  return request<{ taskId: string; status: string; downloadUrl?: string }>({
    url: `/privacy/export/${taskId}`,
    method: 'get'
  });
}
