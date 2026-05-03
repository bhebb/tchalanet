import type { NotificationChannel } from './notification-channel.js';
import type { NotificationSeverity } from './notification-severity.js';

export interface NotificationRecipient {
  channel: NotificationChannel;
  to?: string;
  channelKey?: string;
}

export interface SendNotificationRequest {
  eventId: string;
  tenantCode?: string;
  severity: NotificationSeverity;
  title: string;
  message: string;
  recipients: NotificationRecipient[];
  context?: Record<string, unknown>;
}

export interface NotificationDeliveryResult {
  channel: NotificationChannel;
  to?: string;
  channelKey?: string;
  accepted: boolean;
  reason?: string;
}

export interface SendNotificationResponse {
  accepted: boolean;
  eventId: string;
  deliveries: NotificationDeliveryResult[];
}
