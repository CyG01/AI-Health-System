import { request } from '../request';

/** Get personalized AI recommendations */
export function fetchGetRecommendations() {
  return request<{
    foods: Array<{ id: number; name: string; reason: string; score: number }>;
    exercises: Array<{ id: number; name: string; reason: string; score: number }>;
    tips: string[];
  }>({
    url: '/recommend/personalized',
    method: 'get'
  });
}
