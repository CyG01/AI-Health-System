import request from '@/utils/request'
import { createSSEStream } from '@/utils/sseClient'

export function generatePlan(data) {
  return request({
    url: '/ai-plan/generate',
    method: 'post',
    data
  })
}

export function generatePlanV2(data) {
  return request({
    url: '/ai-plan/generate-v2',
    method: 'post',
    data
  })
}

/**
 * 流式生成 AI 计划（SSE）
 * 通过统一 SSE 客户端处理 token、401 刷新和流解析
 */
export function generatePlanStream(data) {
  let onMessage = null
  let onError = null
  const promise = createSSEStream('/ai-plan/generate-stream', data,
    (text) => { if (onMessage) onMessage(text) },
    (err) => { if (onError) onError(err) }
  )

  return {
    then: (...args) => promise.then(...args),
    catch: (...args) => promise.catch(...args),
    finally: (...args) => promise.finally(...args),
    set onMessage(fn) { onMessage = fn },
    set onError(fn) { onError = fn }
  }
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

export function adjustPlan(data) {
  return request({
    url: '/ai-plan/adjust',
    method: 'post',
    data
  })
}

export function completeTask(detailId) {
  return request({
    url: `/ai-plan/detail/${detailId}/complete`,
    method: 'put'
  })
}
