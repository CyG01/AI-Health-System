// Notification preference functions are now in notification.ts (combined with notifications)
// Re-exporting for backward compatibility
export {
  fetchGetNotificationPreference,
  fetchUpdateNotificationPreference
} from './notification';
