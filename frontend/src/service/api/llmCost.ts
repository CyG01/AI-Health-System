import { request } from '../request';

// ========== LLM Cost Monitoring (Admin) ==========

/** Get global daily cost overview */
export function fetchGetGlobalDailyCost() {
  return request<Api.LlmCost.GlobalDailyCost>({
    url: '/v1/admin/llm-cost/global/daily',
    method: 'get'
  });
}

/** Get user daily cost detail */
export function fetchGetUserDailyCost(userId: number) {
  return request<Api.LlmCost.UserDailyCost>({
    url: `/v1/admin/llm-cost/user/${userId}/daily`,
    method: 'get'
  });
}

/** Get list of users who exceeded their budget */
export function fetchGetOverBudgetUsers() {
  return request<Api.LlmCost.OverBudgetUser[]>({
    url: '/v1/admin/llm-cost/over-budget-users',
    method: 'get'
  });
}

/** Pause a user's LLM calls */
export function fetchPauseUserLlm(userId: number) {
  return request<string>({
    url: `/v1/admin/llm-cost/user/${userId}/pause`,
    method: 'post'
  });
}

/** Resume a user's LLM calls */
export function fetchResumeUserLlm(userId: number) {
  return request<string>({
    url: `/v1/admin/llm-cost/user/${userId}/resume`,
    method: 'post'
  });
}

/** Get model routing status */
export function fetchGetModelStatus() {
  return request<Api.LlmCost.ModelStatus>({
    url: '/v1/admin/llm-cost/model-status',
    method: 'get'
  });
}

/** Get tier circuit breaker status */
export function fetchGetTierCircuitBreakerStatus() {
  return request<Api.LlmCost.TierCircuitBreakerStatus>({
    url: '/v1/admin/llm-cost/tier-circuit-breakers',
    method: 'get'
  });
}
