import request from '@/utils/request'

export function createHealth(data) {
  return request({
    url: '/health/create',
    method: 'post',
    data
  })
}

export function updateHealth(data) {
  return request({
    url: '/health/update',
    method: 'put',
    data
  })
}

export function getLatestHealth() {
  return request({
    url: '/health/get-latest',
    method: 'get'
  })
}

export function getHealthHistory(params) {
  return request({
    url: '/health/history',
    method: 'get',
    params
  })
}

export function getHealthAssessment() {
  return request({
    url: '/health/assessment',
    method: 'get'
  })
}
