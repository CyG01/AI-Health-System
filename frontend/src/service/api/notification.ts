import { request } from '../request';

// ========== Notification ==========

/** Get unread notification count */
export function fetchGetUnreadCount() {
  return request<{ count: number }>({
    url: '/notification/unread-count',
    method: 'get'
  });
}

/** Get notification list */
export function fetchGetNotificationList(params?: { page?: number; size?: number; read?: boolean; type?: string }) {
  return request<Api.Common.PageResult<Api.Notification.Notification>>({
    url: '/notification/list',
    method: 'get',
    params
  });
}

/** Mark a notification as read */
export function fetchMarkAsRead(id: number) {
  return request<void>({
    url: `/notification/${id}/read`,
    method: 'put'
  });
}

/** Mark all notifications as read */
export function fetchMarkAllAsRead() {
  return request<void>({
    url: '/notification/read-all',
    method: 'put'
  });
}

/** Delete a notification */
export function fetchDeleteNotification(id: number) {
  return request<void>({
    url: `/notification/${id}`,
    method: 'delete'
  });
}

// ========== Notification Preference ==========

/** Get notification preferences */
export function fetchGetNotificationPreference() {
  return request<Api.Notification.Preference>({
    url: '/notification-preference',
    method: 'get'
  });
}

/** Update notification preferences */
export function fetchUpdateNotificationPreference(data: Partial<Api.Notification.Preference>) {
  return request<Api.Notification.Preference>({
    url: '/notification-preference',
    method: 'put',
    data
  });
}
