import { request } from '../request';

// ========== User Management ==========

/** Get user list (admin) */
export function fetchGetUserList(params?: Api.Admin.UserListParams) {
  return request<Api.Common.PageResult<Api.Admin.UserItem>>({
    url: '/admin/user/list',
    method: 'get',
    params
  });
}

/** Ban a user (admin) */
export function fetchBanUser(id: number, approvalId?: string) {
  return request<void>({
    url: `/admin/user/${id}/ban`,
    method: 'put',
    headers: approvalId ? { 'X-Approval-Id': approvalId } : {}
  });
}

/** Unban a user (admin) */
export function fetchUnbanUser(id: number, approvalId?: string) {
  return request<void>({
    url: `/admin/user/${id}/unban`,
    method: 'put',
    headers: approvalId ? { 'X-Approval-Id': approvalId } : {}
  });
}

/** Get user detail including profile, plans, checkins, exercise, and diet stats (admin) */
export function fetchGetUserDetail(id: number) {
  return request<Api.Admin.UserDetail>({
    url: `/admin/user/${id}/detail`,
    method: 'get'
  });
}

/** Export user list as CSV (admin, requires approval) */
export function fetchExportUserList(params?: { keyword?: string; status?: number; startDate?: string; endDate?: string }, approvalId?: string) {
  return request<Api.Admin.UserItem[]>({
    url: '/admin/user/export',
    method: 'get',
    params,
    headers: approvalId ? { 'X-Approval-Id': approvalId } : {}
  });
}

// ========== Announcement Management ==========

/** Create announcement (admin) */
export function fetchCreateAnnouncement(data: Api.Admin.AnnouncementParams) {
  return request<Api.Admin.Announcement>({
    url: '/admin/announcement',
    method: 'post',
    data
  });
}

/** Update announcement (admin) */
export function fetchUpdateAnnouncement(data: Api.Admin.AnnouncementParams & { id: number }) {
  return request<Api.Admin.Announcement>({
    url: '/admin/announcement',
    method: 'put',
    data
  });
}

/** Delete announcement (admin) */
export function fetchDeleteAnnouncement(id: number) {
  return request<void>({
    url: `/admin/announcement/${id}`,
    method: 'delete'
  });
}

/** Publish announcement (admin) */
export function fetchPublishAnnouncement(id: number) {
  return request<void>({
    url: `/admin/announcement/${id}/publish`,
    method: 'put'
  });
}

/** Get announcement list (admin) */
export function fetchGetAdminAnnouncementList(params?: { page?: number; size?: number; status?: string }) {
  return request<Api.Common.PageResult<Api.Admin.Announcement>>({
    url: '/admin/announcement/list',
    method: 'get',
    params
  });
}

// ========== Food Dictionary Management ==========

/** Get all food items (admin) */
export function fetchGetAdminFoodItems() {
  return request<Api.Food.FoodItem[]>({
    url: '/admin/food/items',
    method: 'get'
  });
}

/** Create food item (admin) */
export function fetchCreateFoodItem(data: Partial<Api.Food.FoodItem>) {
  return request<Api.Food.FoodItem>({
    url: '/admin/food/item',
    method: 'post',
    data
  });
}

/** Update food item (admin) */
export function fetchUpdateFoodItem(data: Partial<Api.Food.FoodItem> & { id: number }) {
  return request<Api.Food.FoodItem>({
    url: '/admin/food/item',
    method: 'put',
    data
  });
}

/** Delete food item (admin) */
export function fetchDeleteFoodItem(id: number, approvalId?: string) {
  return request<void>({
    url: `/admin/food/item/${id}`,
    method: 'delete',
    headers: approvalId ? { 'X-Approval-Id': approvalId } : {}
  });
}

// ========== Exercise Dictionary Management ==========

/** Get all exercise items (admin) */
export function fetchGetAdminExerciseItems() {
  return request<Api.Exercise.ExerciseItem[]>({
    url: '/admin/exercise/items',
    method: 'get'
  });
}

/** Create exercise item (admin) */
export function fetchCreateExerciseItem(data: Partial<Api.Exercise.ExerciseItem>) {
  return request<Api.Exercise.ExerciseItem>({
    url: '/admin/exercise/item',
    method: 'post',
    data
  });
}

/** Update exercise item (admin) */
export function fetchUpdateExerciseItem(data: Partial<Api.Exercise.ExerciseItem> & { id: number }) {
  return request<Api.Exercise.ExerciseItem>({
    url: '/admin/exercise/item',
    method: 'put',
    data
  });
}

/** Delete exercise item (admin) */
export function fetchDeleteExerciseItem(id: number, approvalId?: string) {
  return request<void>({
    url: `/admin/exercise/item/${id}`,
    method: 'delete',
    headers: approvalId ? { 'X-Approval-Id': approvalId } : {}
  });
}

// ========== Admin Notification ==========

/** Send admin notification */
export function fetchSendAdminNotification(data: Api.Notification.SendParams, approvalId?: string) {
  return request<void>({
    url: '/admin/notification/send',
    method: 'post',
    data,
    headers: approvalId ? { 'X-Approval-Id': approvalId } : {}
  });
}

// ========== Plan Feedback Management ==========

/** Get admin plan feedback list */
export function fetchGetAdminPlanFeedbacks(params?: { page?: number; size?: number }) {
  return request<Api.Common.PageResult<Api.Admin.PlanFeedbackVO>>({
    url: '/admin/plan-feedback/list',
    method: 'get',
    params
  });
}

/** Get admin plan feedback detail */
export function fetchGetAdminPlanFeedbackDetail(id: number) {
  return request<Api.Admin.PlanFeedbackVO>({
    url: `/admin/plan-feedback/${id}`,
    method: 'get'
  });
}

/** Trigger plan adjustment from feedback */
export function fetchTriggerPlanAdjust(id: number) {
  return request<void>({
    url: `/admin/plan-feedback/${id}/adjust`,
    method: 'post'
  });
}

// ========== Audit Logs ==========

/** Get audit logs (admin) */
export function fetchGetAuditLogs(params?: Api.Admin.AuditLogParams) {
  return request<Api.Common.PageResult<Api.Admin.AuditLog>>({
    url: '/admin/audit-log/page',
    method: 'get',
    params
  });
}

// ========== Approval Management ==========

/** Get pending approvals (admin) */
export function fetchGetPendingApprovals() {
  return request<Api.Admin.Approval[]>({
    url: '/admin/approvals/pending',
    method: 'get'
  });
}

/** Approve a request (admin) */
export function fetchApproveRequest(id: number, data: Api.Admin.ApprovalActionParams, adminId: number) {
  return request<void>({
    url: `/admin/approvals/${id}/approve`,
    method: 'post',
    data,
    headers: { 'X-Admin-Id': String(adminId) }
  });
}

/** Reject a request (admin) */
export function fetchRejectRequest(id: number, data: Api.Admin.ApprovalActionParams, adminId: number) {
  return request<void>({
    url: `/admin/approvals/${id}/reject`,
    method: 'post',
    data,
    headers: { 'X-Admin-Id': String(adminId) }
  });
}

/** Submit an approval request */
export function fetchSubmitApprovalRequest(data: { actionType: string; targetDescription: string; operatorId: number; requestPayload?: string }) {
  return request<{ approvalId: number }>({
    url: '/admin/approvals/request',
    method: 'post',
    data,
    headers: { 'X-Admin-Id': String(data.operatorId) }
  });
}

// ========== Rule Suggestion Management ==========

/** Get pending rule suggestions (admin) */
export function fetchGetPendingRuleSuggestions() {
  return request<Api.Admin.RuleSuggestion[]>({
    url: '/admin/rule-suggestions/pending',
    method: 'get'
  });
}

/** Approve rule suggestion (admin) */
export function fetchApproveRuleSuggestion(id: number, reviewerName: string, adminId: number) {
  return request<void>({
    url: `/admin/rule-suggestions/${id}/approve`,
    method: 'post',
    data: { reviewerName },
    headers: { 'X-Admin-Id': String(adminId) }
  });
}

/** Reject rule suggestion (admin) */
export function fetchRejectRuleSuggestion(id: number, reviewerName: string, adminId: number) {
  return request<void>({
    url: `/admin/rule-suggestions/${id}/reject`,
    method: 'post',
    data: { reviewerName },
    headers: { 'X-Admin-Id': String(adminId) }
  });
}

// ========== AI Feedback Management ==========

/** Submit AI feedback */
export function fetchSubmitAiFeedback(data: { userId: number; aiResponseId: string; rating: string; comment: string }) {
  return request<Api.Admin.AiFeedback>({
    url: '/ai/feedback',
    method: 'post',
    data
  });
}

/** Get pending AI feedbacks (admin) */
export function fetchGetPendingAiFeedbacks() {
  return request<Api.Admin.AiFeedback[]>({
    url: '/ai/feedback/pending',
    method: 'get'
  });
}

/** Review AI feedback (admin) */
export function fetchReviewAiFeedback(id: number, result: string) {
  return request<void>({
    url: `/ai/feedback/review/${id}`,
    method: 'post',
    data: { result }
  });
}

// ========== Approval Workflow Utility ==========

/**
 * Wraps a sensitive admin operation with the approval workflow.
 * 1. Submits an approval request to get an approvalId
 * 2. Executes the actual API call with that approvalId
 */
export async function executeWithApproval<T>(
  actionType: string,
  targetDescription: string,
  apiCallFn: (approvalId: string) => Promise<T>,
  operatorId?: number
): Promise<T> {
  const approvalRes = await fetchSubmitApprovalRequest({
    actionType,
    targetDescription,
    operatorId: operatorId ?? 0,
    requestPayload: ''
  });
  const approvalId = approvalRes.data?.approvalId;
  if (!approvalId) {
    throw new Error('Approval request failed: no approvalId returned');
  }
  return apiCallFn(String(approvalId));
}
