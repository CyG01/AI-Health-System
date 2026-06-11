import request from '@/utils/request'

/** 提交血糖记录 */
export function submitBloodSugar(data) {
  return request({
    url: '/blood-sugar/record',
    method: 'post',
    data
  })
}

/** 分页查询血糖记录 */
export function getBloodSugarRecords(params) {
  return request({
    url: '/blood-sugar/records',
    method: 'get',
    params
  })
}

/** 按日期查询血糖记录 */
export function getBloodSugarByDate(date) {
  return request({
    url: `/blood-sugar/records/${date}`,
    method: 'get'
  })
}

/** 血糖趋势（近N天） */
export function getBloodSugarTrend(params) {
  return request({
    url: '/blood-sugar/trend',
    method: 'get',
    params
  })
}

/** 删除血糖记录 */
export function deleteBloodSugar(id) {
  return request({
    url: `/blood-sugar/record/${id}`,
    method: 'delete'
  })
}