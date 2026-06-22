import { request } from '../request';

// ========== AI Plan ==========

/** Generate AI plan */
export function fetchGeneratePlan(data: Api.Plan.GeneratePlanParams) {
  return request<Api.Plan.Plan>({
    url: '/ai-plan/generate',
    method: 'post',
    data
  });
}

/** Generate AI plan V2 */
export function fetchGeneratePlanV2(data: Api.Plan.GeneratePlanParams) {
  return request<Api.Plan.Plan>({
    url: '/ai-plan/generate-v2',
    method: 'post',
    data
  });
}

/** Get plan list (paginated) */
export function fetchGetPlanList(params?: { page?: number; size?: number; keyword?: string }) {
  return request<Api.Common.PageResult<Api.Plan.Plan>>({
    url: '/ai-plan/list',
    method: 'get',
    params
  });
}

/** Get plan detail */
export function fetchGetPlanDetail(id: number) {
  return request<Api.Plan.PlanDetail>({
    url: `/ai-plan/${id}`,
    method: 'get'
  });
}

/** Activate a plan */
export function fetchActivePlan(id: number) {
  return request<void>({
    url: `/ai-plan/${id}/active`,
    method: 'put'
  });
}

/** Delete a plan */
export function fetchDeletePlan(id: number) {
  return request<void>({
    url: `/ai-plan/${id}`,
    method: 'delete'
  });
}

/** Adjust plan with AI (returns SDUI AiAgentResponse) */
export function fetchAdjustPlan(data: { originalPlanId: number; feedback: string }) {
  return request<Record<string, unknown>>({
    url: '/ai-plan/adjust',
    method: 'post',
    data
  });
}

/** Complete a plan day task */
export function fetchCompleteTask(detailId: number) {
  return request<void>({
    url: `/ai-plan/detail/${detailId}/complete`,
    method: 'put'
  });
}

/** Solidify a temporary plan into a formal plan */
export function fetchSolidifyPlan(data: { tempPlanId: number; version: number }) {
  return request<Api.Plan.Plan>({
    url: '/ai-plan/solidify',
    method: 'post',
    data
  });
}

/** Update a single task item within a plan day */
export function fetchUpdateDayItem(planId: number, dayIndex: number, itemIndex: number, newItem: Record<string, unknown>) {
  return request<void>({
    url: `/ai-plan/${planId}/day/${dayIndex}/item/${itemIndex}`,
    method: 'put',
    data: { newItem }
  });
}

/** Replace all task items for a specific day in a plan */
export function fetchReplaceDayItems(planId: number, dayIndex: number, items: Record<string, unknown>[]) {
  return request<void>({
    url: `/ai-plan/${planId}/day/${dayIndex}/items`,
    method: 'put',
    data: { items }
  });
}

/** Replace the entire plan content (Copilot set_plan) */
export function fetchUpdatePlanContent(planId: number, data: { plan?: string; days?: Record<string, unknown>[] }) {
  return request<void>({
    url: `/ai-plan/${planId}/content`,
    method: 'put',
    data
  });
}

// ========== Plan Feedback ==========

/** Submit plan feedback */
export function fetchSubmitPlanFeedback(data: { planId: number; rating: number; content: string }) {
  return request<Api.Plan.PlanFeedback>({
    url: '/plan-feedback',
    method: 'post',
    data
  });
}

/** Get my plan feedbacks */
export function fetchGetMyPlanFeedbacks() {
  return request<Api.Plan.PlanFeedback[]>({
    url: '/plan-feedback/my',
    method: 'get'
  });
}

/** Get plan feedbacks by plan ID */
export function fetchGetPlanFeedbacksByPlanId(planId: number) {
  return request<Api.Plan.PlanFeedback[]>({
    url: `/plan-feedback/plan/${planId}`,
    method: 'get'
  });
}
