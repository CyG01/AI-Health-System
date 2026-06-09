import request from '@/utils/request'

export function getRecommendations() {
  return request({ url: '/recommend/personalized', method: 'get' })
}