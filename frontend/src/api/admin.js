import request from '@/utils/request'

export function getUserList(params) {
  return request({
    url: '/admin/user/list',
    method: 'get',
    params
  })
}

export function banUser(id) {
  return request({
    url: `/admin/user/${id}/ban`,
    method: 'put'
  })
}

export function unbanUser(id) {
  return request({
    url: `/admin/user/${id}/unban`,
    method: 'put'
  })
}

export function createAnnouncement(data) {
  return request({
    url: '/admin/announcement',
    method: 'post',
    data
  })
}

export function updateAnnouncement(data) {
  return request({
    url: '/admin/announcement',
    method: 'put',
    data
  })
}

export function deleteAnnouncement(id) {
  return request({
    url: `/admin/announcement/${id}`,
    method: 'delete'
  })
}

export function publishAnnouncement(id) {
  return request({
    url: `/admin/announcement/${id}/publish`,
    method: 'put'
  })
}

export function getAnnouncementList(params) {
  return request({
    url: '/admin/announcement/list',
    method: 'get',
    params
  })
}
