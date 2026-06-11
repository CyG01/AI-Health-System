import request from '@/utils/request'

export function getExerciseItems(params) {
  return request({
    url: '/exercise/items',
    method: 'get',
    params
  })
}

export function submitExerciseRecord(data) {
  return request({
    url: '/exercise/record',
    method: 'post',
    data
  })
}

export function getExerciseRecordsByUserId(params) {
  return request({
    url: '/exercise/record/user',
    method: 'get',
    params
  })
}

export function getExerciseRecordsByCheckinId(checkinId) {
  return request({
    url: `/exercise/record/checkin/${checkinId}`,
    method: 'get'
  })
}

export function getExerciseRecordsPage(params) {
  return request({
    url: '/exercise/records',
    method: 'get',
    params
  })
}

export function getExerciseRecordsByDate(date) {
  return request({
    url: `/exercise/records/${date}`,
    method: 'get'
  })
}

export function getExerciseGuidance(exerciseItemId) {
  return request({
    url: `/exercise/${exerciseItemId}/guidance`,
    method: 'get'
  })
}

export function updateExerciseRecord(id, data) {
  return request({
    url: `/exercise/record/${id}`,
    method: 'put',
    data
  })
}

export function deleteExerciseRecord(id) {
  return request({
    url: `/exercise/record/${id}`,
    method: 'delete'
  })
}