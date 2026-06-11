import request from '@/utils/request'

export function activateEnterprisePlan(data) {
  return request({
    url: '/enterprise/activate',
    method: 'post',
    data
  })
}

export function updateEnterpriseConfig(data) {
  return request({
    url: '/enterprise/config',
    method: 'put',
    data
  })
}

export function getEnterpriseConfig() {
  return request({
    url: '/enterprise/config',
    method: 'get'
  })
}