import request from '@/utils/request'

/**
 * 用户名密码登录
 */
export function login(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

/**
 * 手机号验证码登录
 */
export function loginByPhone(data) {
  return request({
    url: '/auth/login-by-phone',
    method: 'post',
    data
  })
}

/**
 * 用户注册
 */
export function register(data) {
  return request({
    url: '/auth/register',
    method: 'post',
    data
  })
}

/**
 * 发送短信验证码
 */
export function sendCode(data) {
  return request({
    url: '/auth/send-code',
    method: 'post',
    data
  })
}

/**
 * 重置密码
 */
export function resetPassword(data) {
  return request({
    url: '/auth/reset-password',
    method: 'post',
    data
  })
}

/**
 * 刷新Token
 */
export function refreshToken(refreshTokenValue) {
  return request({
    url: '/auth/refresh',
    method: 'post',
    data: { refreshToken: refreshTokenValue },
    skipAuthRefresh: true
  })
}

/**
 * 退出登录
 */
export function logout() {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}
