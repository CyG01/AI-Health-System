import request from '@/utils/request'

export function getUserProfile() {
  return request({
    url: '/user/profile',
    method: 'get'
  })
}

// 兼容旧名称
export const getProfile = getUserProfile

export function updateProfile(data) {
  return request({
    url: '/user/update-profile',
    method: 'put',
    data
  })
}

export function updatePassword(data) {
  return request({
    url: '/user/update-password',
    method: 'put',
    data
  })
}

export function uploadAvatar(formData) {
  return request({
    url: '/user/upload-avatar',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function deactivateAccount() {
  return request({
    url: '/user/deactivate',
    method: 'delete'
  })
}

export function updateNotificationPreference(data) {
  return request({
    url: '/user/notification-preference',
    method: 'put',
    data
  })
}