import request from '@/utils/request'

export function submitBodyMeasurement(data) {
  return request({ url: '/body-measurement/submit', method: 'post', data })
}

export function getLatestBodyMeasurement() {
  return request({ url: '/body-measurement/latest', method: 'get' })
}

export function getBodyMeasurementHistory(limit = 10) {
  return request({ url: '/body-measurement/history', method: 'get', params: { limit } })
}

export function getBodyMeasurementTrend(months = 6) {
  return request({ url: '/body-measurement/trend', method: 'get', params: { months } })
}

export function deleteBodyMeasurement(id) {
  return request({ url: `/body-measurement/${id}`, method: 'delete' })
}