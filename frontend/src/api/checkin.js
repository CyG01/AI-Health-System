import request from '@/utils/request'

export function submitCheckin(data) {
  return request({
    url: '/checkin/submit',
    method: 'post',
    data
  })
}

export function supplementCheckin(data) {
  return request({
    url: '/checkin/supplement',
    method: 'post',
    data
  })
}

export function getCheckinList(params) {
  return request({
    url: '/checkin/list',
    method: 'get',
    params
  })
}

export function getCheckinStats() {
  return request({
    url: '/checkin/stats',
    method: 'get'
  })
}
