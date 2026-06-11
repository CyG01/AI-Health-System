import request from '@/utils/request'

export function submitWater(data) {
  return request({ url: '/water/submit', method: 'post', data })
}

export function getTodayWater() {
  return request({ url: '/water/today', method: 'get' })
}

export function getWaterList(days = 7) {
  return request({ url: '/water/list', method: 'get', params: { days } })
}

export function getWaterTotal(params) {
  return request({ url: '/water/total', method: 'get', params })
}

export function deleteWater(id) {
  return request({ url: `/water/${id}`, method: 'delete' })
}