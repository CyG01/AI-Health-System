import { request } from '../request';

/** Get weight trend data */
export function fetchGetWeightTrend(params?: { days?: number }) {
  return request<Api.Statistics.TrendData>({
    url: '/statistics/weight',
    method: 'get',
    params
  });
}

/** Get BMI trend data */
export function fetchGetBmiTrend(params?: { days?: number }) {
  return request<Api.Statistics.TrendData>({
    url: '/statistics/bmi',
    method: 'get',
    params
  });
}

/** Get check-in trend data */
export function fetchGetCheckinTrend(params?: { days?: number }) {
  return request<Api.Statistics.CheckinTrendData>({
    url: '/statistics/checkin',
    method: 'get',
    params
  });
}

/** Get exercise trend data */
export function fetchGetExerciseTrend(params?: { days?: number }) {
  return request<Api.Statistics.ExerciseTrendData>({
    url: '/statistics/exercise',
    method: 'get',
    params
  });
}

/** Get calorie trend data */
export function fetchGetCalorieTrend(params?: { days?: number }) {
  return request<Api.Statistics.CalorieTrendData>({
    url: '/statistics/calorie',
    method: 'get',
    params
  });
}

/** Get overall progress summary */
export function fetchGetProgress() {
  return request<Api.Statistics.ProgressData>({
    url: '/statistics/progress',
    method: 'get'
  });
}

/** Get calorie deficit data */
export function fetchGetCalorieDeficit(params?: { days?: number }) {
  return request<Api.Statistics.CalorieDeficitData>({
    url: '/statistics/calorie-deficit',
    method: 'get',
    params
  });
}

/** Get nutrient ratio data */
export function fetchGetNutrientRatio(params?: { days?: number }) {
  return request<Api.Statistics.NameValueData>({
    url: '/statistics/nutrient-ratio',
    method: 'get',
    params
  });
}

/** Get exercise distribution data */
export function fetchGetExerciseDistribution(params?: { days?: number }) {
  return request<Api.Statistics.NameValueData>({
    url: '/statistics/exercise-distribution',
    method: 'get',
    params
  });
}

/** Get diet trend comparison (this week vs last week) */
export function fetchGetDietTrendComparison() {
  return request<Api.Statistics.DietTrendComparisonData>({
    url: '/statistics/diet-trend-comparison',
    method: 'get'
  });
}
