import { request } from '../request';

/** Submit body measurement */
export function fetchSubmitBodyMeasurement(data: Api.Health.BodyMeasurementSubmitRequest) {
  return request<Api.Health.BodyMeasurement>({
    url: '/body-measurement/submit',
    method: 'post',
    data
  });
}

/** Get latest body measurement */
export function fetchGetLatestBodyMeasurement() {
  return request<Api.Health.BodyMeasurement>({
    url: '/body-measurement/latest',
    method: 'get'
  });
}

/** Get body measurement history */
export function fetchGetBodyMeasurementHistory(limit: number = 10) {
  return request<Api.Health.BodyMeasurement[]>({
    url: '/body-measurement/history',
    method: 'get',
    params: { limit }
  });
}

/** Get body measurement trend */
export function fetchGetBodyMeasurementTrend(months: number = 6) {
  return request<Api.Health.BodyMeasurementTrendPoint[]>({
    url: '/body-measurement/trend',
    method: 'get',
    params: { months }
  });
}

/** Delete body measurement */
export function fetchDeleteBodyMeasurement(id: number) {
  return request<void>({
    url: `/body-measurement/${id}`,
    method: 'delete'
  });
}
