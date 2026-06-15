import { request } from '../request';

/** Get dashboard today stats */
export function fetchDashboardToday() {
  return request<Api.Dashboard.DashboardStats>({
    url: '/dashboard/today',
    method: 'get'
  });
}

/** Get dashboard week stats */
export function fetchDashboardWeek() {
  return request<Api.Dashboard.TodayStats>({
    url: '/dashboard/week',
    method: 'get'
  });
}

/** Get dashboard month stats */
export function fetchDashboardMonth() {
  return request<Api.Dashboard.TodayStats>({
    url: '/dashboard/month',
    method: 'get'
  });
}

/** Get AI predictive greeting card */
export function fetchGreeting() {
  return request<Api.Dashboard.GreetingCard>({
    url: '/dashboard/greeting',
    method: 'get'
  });
}
