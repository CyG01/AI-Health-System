import request from '@/utils/request'

export function submitSleep(data) {
  return request({ url: '/sleep/submit', method: 'post', data })
}

export function getTodaySleep() {
  return request({ url: '/sleep/today', method: 'get' })
}

export function getSleepList(days = 30) {
  return request({ url: '/sleep/list', method: 'get', params: { days } })
}

export function analyzeSleep() {
  return request({ url: '/sleep/analyze', method: 'get' })
}