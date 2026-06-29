import { request } from '../request';

/** Submit water intake record */
export function fetchSubmitWater(data: Api.Water.CreateWaterParams) {
  return request<Api.Water.WaterRecord>({
    url: '/water/submit',
    method: 'post',
    data
  });
}

/** Get today's water intake */
export function fetchGetTodayWater() {
  return request<Api.Water.DailyWaterSummary>({
    url: '/water/today',
    method: 'get'
  });
}

/** Get water intake list */
export function fetchGetWaterList(days: number = 7) {
  return request<Api.Water.WaterRecord[]>({
    url: '/water/list',
    method: 'get',
    params: { days }
  });
}

/** Get water total for a specific date */
export function fetchGetWaterTotal(params: { date: string }) {
  return request<{ date: string; totalMl: number }>({
    url: '/water/total',
    method: 'get',
    params
  });
}

/** Delete water record */
export function fetchDeleteWater(id: number) {
  return request<void>({
    url: `/water/${id}`,
    method: 'delete'
  });
}
