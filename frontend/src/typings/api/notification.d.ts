declare namespace Api {
  namespace Notification {
    /** Notification — aligned with backend NotificationVO */
    interface Notification {
      id: number;
      title: string;
      content: string;
      type: string;
      isRead: number;
      createTime: string;
    }

    interface NotificationItem {
      id: number;
      title: string;
      content: string;
      type: string;
      isRead: number;
      createTime: string;
    }

    interface NotificationList {
      items: Notification[];
      total: number;
      unreadCount: number;
    }

    interface SendParams {
      title: string;
      content: string;
      userIds?: number[];
      type?: string;
    }

    /** Notification preference — aligned with backend NotificationPreferenceVO */
    interface NotificationPreference {
      notificationEnabled: number;
      reminderTime: string;
      notifyExercise: number;
      notifyDiet: number;
      notifyCheckin: number;
      quietStart: string;
      quietEnd: string;
    }

    interface Preference {
      notificationEnabled: number;
      reminderTime: string;
      notifyExercise: number;
      notifyDiet: number;
      notifyCheckin: number;
      quietStart: string;
      quietEnd: string;
    }
  }
}
