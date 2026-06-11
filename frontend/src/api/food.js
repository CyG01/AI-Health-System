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

export function recognizeFood(formData) {
  return request({
    url: '/food/recognize',
    method: 'post',
    data: formData,
    timeout: 30000
  })
}

/** 查询用户常用食物 */
export function getFrequentItems(params) {
  return request({
    url: '/food/items/frequent',
    method: 'get',
    params
  })
}

/** 文字快捷录入食物 */
export function parseFoodText(params) {
  return request({
    url: '/food/items/parse',
    method: 'get',
    params
  })
}

export function updateFoodRecord(id, data) {
  return request({
    url: `/food/record/${id}`,
    method: 'put',
    data
  })
}

export function deleteFoodRecord(id) {
  return request({
    url: `/food/record/${id}`,
    method: 'delete'
  })
}