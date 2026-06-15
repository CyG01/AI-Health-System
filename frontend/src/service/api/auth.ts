import { request } from '../request';

/** Login */
export function fetchLogin(params: Api.Auth.LoginParams) {
  return request<Api.Auth.LoginToken>({ url: '/auth/login', method: 'post', data: params });
}

/** Login by phone */
export function fetchLoginByPhone(params: Api.Auth.LoginByPhoneParams) {
  return request<Api.Auth.LoginToken>({ url: '/auth/login-by-phone', method: 'post', data: params });
}

/** Register */
export function fetchRegister(params: Api.Auth.RegisterRequest) {
  return request<void>({ url: '/auth/register', method: 'post', data: params });
}

/** Get captcha */
export function fetchCaptcha() {
  return request<Api.Auth.CaptchaResponse>({ url: '/auth/captcha', method: 'get' });
}

/** Send verification code */
export function fetchSendCode(params: Api.Auth.SendCodeParams) {
  return request<void>({ url: '/auth/send-code', method: 'post', data: params });
}

/** Reset password */
export function fetchResetPassword(params: Api.Auth.ResetPasswordRequest) {
  return request<void>({ url: '/auth/reset-password', method: 'post', data: params });
}

/** Refresh token */
export function fetchRefreshToken(refreshToken: string) {
  return request<Api.Auth.LoginToken>({
    url: '/auth/refresh',
    method: 'post',
    headers: { 'Refresh-Token': `Bearer ${refreshToken}` }
  });
}

/** Logout */
export function fetchLogout(refreshToken?: string) {
  return request<void>({
    url: '/auth/logout',
    method: 'post',
    headers: refreshToken ? { 'Refresh-Token': `Bearer ${refreshToken}` } : {}
  });
}

/** Get user info (used by soybean-admin auth store) */
export function fetchGetUserInfo() {
  return request<Api.Auth.RawUserInfo>({ url: '/user/profile', method: 'get' });
}
