import request from '@/utils/request'

export function submitPlanFeedback(data) {
  return request({
    url: '/plan-feedback',
    method: 'post',
    data
  })
}

export function getMyPlanFeedbacks() {
  return request({
    url: '/plan-feedback/my',
    method: 'get'
  })
}

export function getPlanFeedbacksByPlanId(planId) {
  return request({
    url: `/plan-feedback/plan/${planId}`,
    method: 'get'
  })
}