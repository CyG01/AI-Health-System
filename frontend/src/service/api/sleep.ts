import { request } from '../request';

/** Submit sleep record */
export function fetchSubmitSleep(data: Api.Sleep.CreateSleepParams) {
  return request<Api.Sleep.SleepRecord>({
    url: '/sleep/submit',
    method: 'post',
    data
  });
}

/** Get today's sleep record */
export function fetchGetTodaySleep() {
  return request<Api.Sleep.SleepRecord>({
    url: '/sleep/today',
    method: 'get'
  });
}

/** Get sleep list */
export function fetchGetSleepList(days: number = 30) {
  return request<Api.Sleep.SleepRecord[]>({
    url: '/sleep/list',
    method: 'get',
    params: { days }
  });
}

/** Analyze sleep patterns */
export function fetchAnalyzeSleep() {
  return request<Api.Sleep.SleepStats>({
    url: '/sleep/analyze',
    method: 'get'
  });
}

/** Delete sleep record */
export function fetchDeleteSleep(id: number) {
  return request<void>({
    url: `/sleep/${id}`,
    method: 'delete'
  });
}
