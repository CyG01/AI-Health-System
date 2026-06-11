import request from '@/utils/request'

export function getUserList(params) {
  return request({
    url: '/admin/user/list',
    method: 'get',
    params
  })
}

export function banUser(id) {
  return request({
    url: `/admin/user/${id}/ban`,
    method: 'put'
  })
}

export function unbanUser(id) {
  return request({
    url: `/admin/user/${id}/unban`,
    method: 'put'
  })
}

export function createAnnouncement(data) {
  return request({
    url: '/admin/announcement',
    method: 'post',
    data
  })
}

export function updateAnnouncement(data) {
  return request({
    url: '/admin/announcement',
    method: 'put',
    data
  })
}

export function deleteAnnouncement(id) {
  return request({
    url: `/admin/announcement/${id}`,
    method: 'delete'
  })
}

export function publishAnnouncement(id) {
  return request({
    url: `/admin/announcement/${id}/publish`,
    method: 'put'
  })
}

export function getAnnouncementList(params) {
  return request({
    url: '/admin/announcement/list',
    method: 'get',
    params
  })
}

// ========== 管理员食物字典管理 ==========
export function getAdminFoodItems() {
  return request({
    url: '/admin/food/items',
    method: 'get'
  })
}

export function createFoodItem(data) {
  return request({
    url: '/admin/food/item',
    method: 'post',
    data
  })
}

export function updateFoodItem(data) {
  return request({
    url: '/admin/food/item',
    method: 'put',
    data
  })
}

export function deleteFoodItem(id) {
  return request({
    url: `/admin/food/item/${id}`,
    method: 'delete'
  })
}

// ========== 管理员运动字典管理 ==========
export function getAdminExerciseItems() {
  return request({
    url: '/admin/exercise/items',
    method: 'get'
  })
}

export function createExerciseItem(data) {
  return request({
    url: '/admin/exercise/item',
    method: 'post',
    data
  })
}

export function updateExerciseItem(data) {
  return request({
    url: '/admin/exercise/item',
    method: 'put',
    data
  })
}

export function deleteExerciseItem(id) {
  return request({
    url: `/admin/exercise/item/${id}`,
    method: 'delete'
  })
}

// ========== 管理员通知管理 ==========
export function sendAdminNotification(data) {
  return request({
    url: '/admin/notification/send',
    method: 'post',
    data
  })
}

// ========== 管理员计划反馈管理 ==========
export function getAdminPlanFeedbacks(params) {
  return request({
    url: '/admin/plan-feedback/list',
    method: 'get',
    params
  })
}

export function getAdminPlanFeedbackDetail(id) {
  return request({
    url: `/admin/plan-feedback/${id}`,
    method: 'get'
  })
}

export function triggerPlanAdjust(id) {
  return request({
    url: `/admin/plan-feedback/${id}/adjust`,
    method: 'post'
  })
}

// ========== 管理员审计日志 ==========
export function getAuditLogs(params) {
  return request({
    url: '/admin/audit-log/page',
    method: 'get',
    params
  })
}

// ========== 管理员审批管理 ==========
export function getPendingApprovals() {
  return request({
    url: '/admin/approvals/pending',
    method: 'get'
  })
}

export function approveRequest(id, data) {
  return request({
    url: `/admin/approvals/${id}/approve`,
    method: 'post',
    data,
    headers: { 'X-Admin-Id': data.approverId }
  })
}

export function rejectRequest(id, data) {
  return request({
    url: `/admin/approvals/${id}/reject`,
    method: 'post',
    data,
    headers: { 'X-Admin-Id': data.approverId }
  })
}

export function submitApprovalRequest(data) {
  return request({
    url: '/admin/approvals/request',
    method: 'post',
    data,
    headers: { 'X-Admin-Id': data.operatorId }
  })
}

// ========== 管理员规则建议管理 ==========
export function getPendingRuleSuggestions() {
  return request({
    url: '/admin/rule-suggestions/pending',
    method: 'get'
  })
}

export function approveRuleSuggestion(id, reviewerName, adminId) {
  return request({
    url: `/admin/rule-suggestions/${id}/approve`,
    method: 'post',
    data: { reviewerName },
    headers: { 'X-Admin-Id': adminId }
  })
}

export function rejectRuleSuggestion(id, reviewerName, adminId) {
  return request({
    url: `/admin/rule-suggestions/${id}/reject`,
    method: 'post',
    data: { reviewerName },
    headers: { 'X-Admin-Id': adminId }
  })
}
