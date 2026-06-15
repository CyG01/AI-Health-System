import { request } from '../request';

/** Submit blood sugar record */
export function fetchSubmitBloodSugar(data: Api.BloodSugar.CreateParams) {
  return request<Api.BloodSugar.Record>({
    url: '/blood-sugar/record',
    method: 'post',
    data
  });
}

/** Get blood sugar records (paginated) */
export function fetchGetBloodSugarRecords(params?: { page?: number; size?: number }) {
  return request<Api.Common.PageResult<Api.BloodSugar.Record>>({
    url: '/blood-sugar/records',
    method: 'get',
    params
  });
}

/** Get blood sugar records by date */
export function fetchGetBloodSugarByDate(date: string) {
  return request<Api.BloodSugar.Record[]>({
    url: `/blood-sugar/records/${date}`,
    method: 'get'
  });
}

/** Get blood sugar trend data */
export function fetchGetBloodSugarTrend(params?: { days?: number }) {
  return request<Api.BloodSugar.Record[]>({
    url: '/blood-sugar/trend',
    method: 'get',
    params
  });
}

/** Delete blood sugar record */
export function fetchDeleteBloodSugar(id: number) {
  return request<void>({
    url: `/blood-sugar/record/${id}`,
    method: 'delete'
  });
}
