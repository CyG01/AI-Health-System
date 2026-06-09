import request from '@/utils/request'

export function getDashboardToday() {
  return request({
    url: '/dashboard/today',
    method: 'get'
  })
}

export function getDashboardWeek() {
  return request({
    url: '/dashboard/week',
    method: 'get'
  })
}

export function getDashboardMonth() {
  return request({
    url: '/dashboard/month',
    method: 'get'
  })
}