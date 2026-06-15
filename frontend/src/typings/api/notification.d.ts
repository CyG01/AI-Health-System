declare namespace Api {
  namespace Notification {
    interface Notification {
      id: number;
      userId: number;
      title: string;
      content: string;
      type: string;
      read: boolean;
      createdAt: string;
    }

    interface NotificationItem {
      id: number;
      userId: number;
      title: string;
      content: string;
      type: string;
      read: boolean;
      createdAt: string;
    }

    interface NotificationList {
      items: Notification[];
      total: number;
      unreadCount: number;
    }

    interface NotificationListParams {
      page?: number;
      size?: number;
      read?: boolean;
      type?: string;
    }

    interface UnreadCount {
      count: number;
    }

    interface SendParams {
      title: string;
      content: string;
      userIds?: number[];
      type?: string;
    }

    interface NotificationPreference {
      emailEnabled: boolean;
      pushEnabled: boolean;
      smsEnabled: boolean;
      quietHoursStart?: string;
      quietHoursEnd?: string;
    }

    interface NotificationPreferenceUpdateRequest {
      emailEnabled?: boolean;
      pushEnabled?: boolean;
      smsEnabled?: boolean;
      quietHoursStart?: string;
      quietHoursEnd?: string;
    }

    interface Preference {
      emailEnabled: boolean;
      pushEnabled: boolean;
      smsEnabled: boolean;
      quietHoursStart?: string;
      quietHoursEnd?: string;
    }
  }
}
