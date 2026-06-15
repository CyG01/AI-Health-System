import { request } from '../request';

/** Get weight trend data */
export function fetchGetWeightTrend(params?: { days?: number; startDate?: string; endDate?: string }) {
  return request<Api.Statistics.TrendPoint[]>({
    url: '/statistics/weight',
    method: 'get',
    params
  });
}

/** Get BMI trend data */
export function fetchGetBmiTrend(params?: { days?: number; startDate?: string; endDate?: string }) {
  return request<Api.Statistics.TrendPoint[]>({
    url: '/statistics/bmi',
    method: 'get',
    params
  });
}

/** Get check-in trend data */
export function fetchGetCheckinTrend(params?: { days?: number; startDate?: string; endDate?: string }) {
  return request<Array<{ date: string; checked: boolean }>>({
    url: '/statistics/checkin',
    method: 'get',
    params
  });
}

/** Get exercise trend data */
export function fetchGetExerciseTrend(params?: { days?: number; startDate?: string; endDate?: string }) {
  return request<Api.Statistics.TrendPoint[]>({
    url: '/statistics/exercise',
    method: 'get',
    params
  });
}

/** Get calorie trend data */
export function fetchGetCalorieTrend(params?: { days?: number; startDate?: string; endDate?: string }) {
  return request<Array<{ date: string; intake: number; burned: number; net: number }>>({
    url: '/statistics/calorie',
    method: 'get',
    params
  });
}

/** Get overall progress summary */
export function fetchGetProgress() {
  return request<{
    weightChange: number;
    bmiChange: number;
    totalExerciseMinutes: number;
    totalCaloriesBurned: number;
    checkinRate: number;
    planCompletionRate: number;
  }>({
    url: '/statistics/progress',
    method: 'get'
  });
}

/** Get calorie deficit data */
export function fetchGetCalorieDeficit(params?: { days?: number; startDate?: string; endDate?: string }) {
  return request<Array<{ date: string; deficit: number; cumulative: number }>>({
    url: '/statistics/calorie-deficit',
    method: 'get',
    params
  });
}

/** Get nutrient ratio data */
export function fetchGetNutrientRatio(params?: { days?: number; startDate?: string; endDate?: string }) {
  return request<Array<{ protein: number; carbs: number; fat: number; date: string }>>({
    url: '/statistics/nutrient-ratio',
    method: 'get',
    params
  });
}

/** Get exercise distribution data */
export function fetchGetExerciseDistribution(params?: { days?: number; startDate?: string; endDate?: string }) {
  return request<Array<{ category: string; totalMinutes: number; percentage: number }>>({
    url: '/statistics/exercise-distribution',
    method: 'get',
    params
  });
}

/** Get diet trend comparison (this week vs last week) */
export function fetchGetDietTrendComparison() {
  return request<{
    thisWeek: Array<{ date: string; intake: number; burned: number; net: number }>;
    lastWeek: Array<{ date: string; intake: number; burned: number; net: number }>;
    changePercent: number;
  }>({
    url: '/statistics/diet-trend-comparison',
    method: 'get'
  });
}
