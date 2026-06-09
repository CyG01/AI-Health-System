import request from '@/utils/request'

export function getCaptcha() {
  return request({
    url: '/auth/captcha',
    method: 'get',
    skipAuthRefresh: true
  })
}

export function login(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data,
    skipAuthRefresh: true
  })
}

export function loginByPhone(data) {
  return request({
    url: '/auth/login-by-phone',
    method: 'post',
    data,
    skipAuthRefresh: true
  })
}

export function register(data) {
  return request({
    url: '/auth/register',
    method: 'post',
    data,
    skipAuthRefresh: true
  })
}

export function sendCode(data) {
  return request({
    url: '/auth/send-code',
    method: 'post',
    data,
    skipAuthRefresh: true
  })
}

export function resetPassword(data) {
  return request({
    url: '/auth/reset-password',
    method: 'post',
    data,
    skipAuthRefresh: true
  })
}

export function refreshToken(refreshTokenValue) {
  return request({
    url: '/auth/refresh',
    method: 'post',
    headers: {
      'Refresh-Token': `Bearer ${refreshTokenValue}`
    },
    skipAuthRefresh: true
  })
}

export function logout() {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}
