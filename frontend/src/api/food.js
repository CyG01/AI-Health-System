import request from '@/utils/request'

export function getFoodItems(params) {
  return request({
    url: '/food/items',
    method: 'get',
    params
  })
}

export function submitFoodRecord(data) {
  return request({
    url: '/food/record',
    method: 'post',
    data
  })
}

export function getFoodRecordsByUserId(params) {
  return request({
    url: '/food/record/user',
    method: 'get',
    params
  })
}

export function getFoodRecordsByCheckinId(checkinId) {
  return request({
    url: `/food/record/checkin/${checkinId}`,
    method: 'get'
  })
}

export function getFoodRecordsPage(params) {
  return request({
    url: '/food/records',
    method: 'get',
    params
  })
}

export function getFoodRecordsByDate(date) {
  return request({
    url: `/food/records/${date}`,
    method: 'get'
  })
}

export function recognizeFood(data) {
  return request({
    url: '/food-recognition/recognize',
    method: 'post',
    data,
    timeout: 30000
  })
}