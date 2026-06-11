import request from '@/utils/request'

export function createGoal(data) {
  return request({ url: '/goal/create', method: 'post', data })
}

export function updateGoal(data) {
  return request({ url: '/goal/update', method: 'put', data })
}

export function deleteGoal(goalId) {
  return request({ url: `/goal/${goalId}`, method: 'delete' })
}

export function getGoalList() {
  return request({ url: '/goal/list', method: 'get' })
}

export function getGoalDetail(goalId) {
  return request({ url: `/goal/${goalId}`, method: 'get' })
}

export function updateGoalStatus(goalId, status) {
  return request({ url: `/goal/${goalId}/status`, method: 'put', data: { status } })
}