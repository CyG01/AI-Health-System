import { request } from '../request';

// ========== LLM Ops Prompt Management (Admin) ==========

/** Activate a specific prompt version */
export function fetchActivatePromptVersion(templateKey: string, version: number) {
  return request<void>({
    url: `/admin/llmops/prompt/${templateKey}/activate/${version}`,
    method: 'post'
  });
}

/** Rollback prompt to previous active version */
export function fetchRollbackPrompt(templateKey: string) {
  return request<void>({
    url: `/admin/llmops/prompt/${templateKey}/rollback`,
    method: 'post'
  });
}

/** Start A/B test between two prompt versions */
export function fetchStartPromptAbTest(templateKey: string, params: { versionA: number; versionB: number; ratioA: number }) {
  return request<void>({
    url: `/admin/llmops/prompt/${templateKey}/ab-test`,
    method: 'post',
    params
  });
}

/** Get prompt version history */
export function fetchPromptHistory(templateKey: string) {
  return request<Api.LlmOps.PromptVersion[]>({
    url: `/admin/llmops/prompt/${templateKey}/history`,
    method: 'get'
  });
}

/** Get current active prompt version */
export function fetchActivePromptVersion(templateKey: string) {
  return request<Api.LlmOps.ActiveVersion>({
    url: `/admin/llmops/prompt/${templateKey}/active`,
    method: 'get'
  });
}

// ========== Canary Deployment ==========

/** Start canary deployment for a prompt version */
export function fetchStartCanary(templateKey: string, params: { version: number; percentage: number }) {
  return request<void>({
    url: `/admin/llmops/prompt/${templateKey}/canary/start`,
    method: 'post',
    params
  });
}

/** Increase canary traffic percentage */
export function fetchIncreaseCanary(templateKey: string, params: { percentage: number }) {
  return request<void>({
    url: `/admin/llmops/prompt/${templateKey}/canary/increase`,
    method: 'post',
    params
  });
}

/** Complete canary deployment (promote to 100%) */
export function fetchCompleteCanary(templateKey: string) {
  return request<void>({
    url: `/admin/llmops/prompt/${templateKey}/canary/complete`,
    method: 'post'
  });
}

/** Cancel canary deployment (rollback) */
export function fetchCancelCanary(templateKey: string) {
  return request<void>({
    url: `/admin/llmops/prompt/${templateKey}/canary/cancel`,
    method: 'post'
  });
}

/** Get canary deployment status */
export function fetchCanaryStatus(templateKey: string) {
  return request<Api.LlmOps.CanaryStatus>({
    url: `/admin/llmops/prompt/${templateKey}/canary/status`,
    method: 'get'
  });
}

/** Get all running canary deployments */
export function fetchRunningCanaries() {
  return request<Api.LlmOps.CanaryStatus[]>({
    url: '/admin/llmops/prompt/canaries/running',
    method: 'get'
  });
}

// ========== Model & Infrastructure Monitoring ==========

/** Get LLM model status */
export function fetchLlmOpsModelStatus() {
  return request<Record<string, unknown>>({
    url: '/admin/llmops/models/status',
    method: 'get'
  });
}

/** Get LLM Ops alerts */
export function fetchLlmOpsAlerts(limit?: number) {
  return request<Api.LlmOps.Alert[]>({
    url: '/admin/llmops/alerts',
    method: 'get',
    params: { limit }
  });
}

/** Get circuit breaker status */
export function fetchCircuitStatus() {
  return request<Api.LlmOps.CircuitStatus>({
    url: '/admin/llmops/circuit/status',
    method: 'get'
  });
}

/** Get LLM Ops dashboard overview */
export function fetchLlmOpsDashboard() {
  return request<Record<string, unknown>>({
    url: '/admin/llmops/dashboard',
    method: 'get'
  });
}
