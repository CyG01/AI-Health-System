import { request } from '../request';

/** Submit daily check-in */
export function fetchSubmitCheckin(data: Api.Checkin.CheckinParams) {
  return request<Api.Checkin.CheckinRecord>({
    url: '/checkin/submit',
    method: 'post',
    data
  });
}

/** Supplement check-in (make-up for missed day) */
export function fetchSupplementCheckin(data: Api.Checkin.CheckinParams & { date: string; reason: string }) {
  return request<Api.Checkin.CheckinRecord>({
    url: '/checkin/supplement',
    method: 'post',
    data
  });
}

/** Get check-in list */
export function fetchGetCheckinList(params?: { page?: number; size?: number; startDate?: string; endDate?: string }) {
  return request<Api.Checkin.CheckinRecord[]>({
    url: '/checkin/list',
    method: 'get',
    params
  });
}

/** Get check-in statistics */
export function fetchGetCheckinStats() {
  return request<Api.Checkin.CalendarData>({
    url: '/checkin/stats',
    method: 'get'
  });
}

/** Get check-in page (paginated) */
export function fetchGetCheckinPage(params: { page: number; size: number }) {
  return request<Api.Common.PageResult<Api.Checkin.CheckinRecord>>({
    url: '/checkin/page',
    method: 'get',
    params
  });
}

/** Get today's check-in record */
export function fetchGetTodayCheckin() {
  return request<Api.Checkin.CheckinRecord>({
    url: '/checkin/today',
    method: 'get'
  });
}
