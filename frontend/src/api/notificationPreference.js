import request from '@/utils/request'

export function getNotificationPreference() {
  return request({
    url: '/notification-preference',
    method: 'get'
  })
}

export function updateNotificationPreference(data) {
  return request({
    url: '/notification-preference',
    method: 'put',
    data
  })
}