import request from '@/utils/request'

/**
 * 获取当前用户个人信息
 */
export function getProfile() {
  return request({
    url: '/auth/profile',
    method: 'get'
  })
}

/**
 * 更新个人信息
 */
export function updateProfile(data) {
  return request({
    url: '/auth/update-profile',
    method: 'put',
    data
  })
}

/**
 * 修改密码
 */
export function updatePassword(data) {
  return request({
    url: '/auth/update-password',
    method: 'put',
    data
  })
}

/**
 * 上传头像
 */
export function uploadAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/auth/upload-avatar',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
