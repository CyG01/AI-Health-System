import request from '@/utils/request'

export function generatePlan(data) {
  return request({
    url: '/ai-plan/generate',
    method: 'post',
    data
  })
}

export function getPlanList() {
  return request({
    url: '/ai-plan/list',
    method: 'get'
  })
}

export function getPlanDetail(id) {
  return request({
    url: `/ai-plan/${id}`,
    method: 'get'
  })
}

export function activePlan(id) {
  return request({
    url: `/ai-plan/${id}/active`,
    method: 'put'
  })
}

export function deletePlan(id) {
  return request({
    url: `/ai-plan/${id}`,
    method: 'delete'
  })
}
