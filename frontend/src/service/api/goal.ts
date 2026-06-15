import { request } from '../request';

/** Create a goal */
export function fetchCreateGoal(data: Api.Goal.GoalCreateRequest) {
  return request<Api.Goal.GoalItem>({
    url: '/goal/create',
    method: 'post',
    data
  });
}

/** Update a goal */
export function fetchUpdateGoal(data: { id: number; title?: string; description?: string; targetDate?: string }) {
  return request<Api.Goal.GoalItem>({
    url: '/goal/update',
    method: 'put',
    data
  });
}

/** Delete a goal */
export function fetchDeleteGoal(goalId: number) {
  return request<void>({
    url: `/goal/${goalId}`,
    method: 'delete'
  });
}

/** Get goal list */
export function fetchGetGoalList() {
  return request<Api.Goal.GoalItem[]>({
    url: '/goal/list',
    method: 'get'
  });
}

/** Get goal detail */
export function fetchGetGoalDetail(goalId: number) {
  return request<Api.Goal.GoalItem>({
    url: `/goal/${goalId}`,
    method: 'get'
  });
}

/** Update goal status */
export function fetchUpdateGoalStatus(goalId: number, status: string) {
  return request<void>({
    url: `/goal/${goalId}/status`,
    method: 'put',
    data: { status }
  });
}
