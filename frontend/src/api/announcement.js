import request from '@/utils/request'

export function getAnnouncementList(params) {
  return request({
    url: '/announcement/list',
    method: 'get',
    params
  })
}

export function getAnnouncementDetail(id) {
  return request({
    url: `/announcement/${id}`,
    method: 'get'
  })
}