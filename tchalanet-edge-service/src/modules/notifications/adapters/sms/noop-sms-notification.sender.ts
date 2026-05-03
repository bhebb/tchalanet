import type {
  NotificationRecipient,
  SendNotificationRequest,
} from '../../domain/notification-message.js';
import type { NotificationSender } from '../../ports/notification-sender.port.js';

export class NoopSmsNotificationSender implements NotificationSender {
  supports(recipient: NotificationRecipient): boolean {
    return recipient.channel === 'SMS';
  }

  async send(
    notification: SendNotificationRequest,
    recipient: NotificationRecipient,
  ): Promise<void> {
    console.log(
      `[noop-sms] to=${recipient.to} body=[${notification.severity}] ${notification.title}`,
    );
  }
}
