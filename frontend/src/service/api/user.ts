import { request } from '../request';

/** Get user profile */
export function fetchGetUserProfile() {
  return request<Api.Auth.UserInfo>({
    url: '/user/profile',
    method: 'get'
  });
}

/** Update user profile */
export function fetchUpdateProfile(data: Partial<Api.Auth.UserInfo>) {
  return request<void>({
    url: '/user/update-profile',
    method: 'put',
    data
  });
}

/** Update password */
export function fetchUpdatePassword(data: { oldPassword: string; newPassword: string; confirmPassword: string }) {
  return request<void>({
    url: '/user/update-password',
    method: 'put',
    data
  });
}

/** Upload avatar */
export function fetchUploadAvatar(formData: FormData) {
  return request<string>({
    url: '/user/upload-avatar',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  });
}

/** Deactivate account */
export function fetchDeactivateAccount() {
  return request<void>({
    url: '/user/deactivate',
    method: 'delete'
  });
}
