import request from '@/utils/request'

export function getWeightTrend(params) {
  return request({
    url: '/statistics/weight',
    method: 'get',
    params
  })
}

export function getBmiTrend(params) {
  return request({
    url: '/statistics/bmi',
    method: 'get',
    params
  })
}

export function getCheckinTrend(params) {
  return request({
    url: '/statistics/checkin',
    method: 'get',
    params
  })
}

export function getExerciseTrend(params) {
  return request({
    url: '/statistics/exercise',
    method: 'get',
    params
  })
}

export function getCalorieTrend(params) {
  return request({
    url: '/statistics/calorie',
    method: 'get',
    params
  })
}

export function getProgress() {
  return request({
    url: '/statistics/progress',
    method: 'get'
  })
}
