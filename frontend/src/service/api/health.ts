import { request } from '../request';

/** Create health record */
export function fetchCreateHealth(data: Api.Health.CreateHealthParams) {
  return request<Api.Health.HealthRecord>({
    url: '/health/create',
    method: 'post',
    data
  });
}

/** Update health record */
export function fetchUpdateHealth(data: Api.Health.UpdateHealthParams) {
  return request<Api.Health.HealthRecord>({
    url: '/health/update',
    method: 'put',
    data
  });
}

/** Get latest health record */
export function fetchGetLatestHealth() {
  return request<Api.Health.HealthRecord>({
    url: '/health/get-latest',
    method: 'get'
  });
}

/** Get health history */
export function fetchGetHealthHistory(params?: { page?: number; size?: number }) {
  return request<Api.Health.HealthHistoryRecord[]>({
    url: '/health/history',
    method: 'get',
    params
  });
}

/** Get health assessment */
export function fetchGetHealthAssessment() {
  return request<Api.Health.HealthAssessment>({
    url: '/health/assessment',
    method: 'get'
  });
}

/** Get health progress towards goals */
export function fetchGetHealthProgress() {
  return request<Api.Health.HealthProgress>({
    url: '/health/progress',
    method: 'get'
  });
}
