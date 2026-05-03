import type {
  NotificationRecipient,
  SendNotificationRequest,
} from '../domain/notification-message.js';

export interface NotificationSender {
  supports(recipient: NotificationRecipient): boolean;
  send(notification: SendNotificationRequest, recipient: NotificationRecipient): Promise<void>;
}
