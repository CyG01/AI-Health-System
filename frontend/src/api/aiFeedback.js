import request from '@/utils/request'

/**
 * 提交 AI 反馈评价
 */
export function submitAiFeedback(data) {
  return request({
    url: '/ai/feedback',
    method: 'post',
    data
  })
}

/**
 * 获取待审核反馈列表（管理员）
 */
export function getPendingAiFeedbacks() {
  return request({
    url: '/ai/feedback/pending',
    method: 'get'
  })
}

/**
 * 审核反馈（管理员）
 */
export function reviewAiFeedback(id, result) {
  return request({
    url: `/ai/feedback/review/${id}`,
    method: 'post',
    data: { result }
  })
}