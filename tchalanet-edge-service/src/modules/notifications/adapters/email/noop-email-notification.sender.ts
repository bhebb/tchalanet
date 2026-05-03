import type {
  NotificationRecipient,
  SendNotificationRequest,
} from '../../domain/notification-message.js';
import type { NotificationSender } from '../../ports/notification-sender.port.js';

export class NoopEmailNotificationSender implements NotificationSender {
  supports(recipient: NotificationRecipient): boolean {
    return recipient.channel === 'EMAIL';
  }

  async send(
    notification: SendNotificationRequest,
    recipient: NotificationRecipient,
  ): Promise<void> {
    console.log(
      `[noop-email] to=${recipient.to} subject=[${notification.severity}] ${notification.title}`,
    );
  }
}
