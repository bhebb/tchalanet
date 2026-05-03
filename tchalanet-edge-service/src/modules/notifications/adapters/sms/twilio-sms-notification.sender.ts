import twilio from 'twilio';

import type {
  NotificationRecipient,
  SendNotificationRequest,
} from '../../domain/notification-message.js';
import type { NotificationSender } from '../../ports/notification-sender.port.js';

export class TwilioSmsNotificationSender implements NotificationSender {
  constructor(
    private readonly enabled: boolean,
    private readonly accountSid: string,
    private readonly authToken: string,
    private readonly from: string,
  ) {}

  supports(recipient: NotificationRecipient): boolean {
    return recipient.channel === 'SMS';
  }

  async send(
    notification: SendNotificationRequest,
    recipient: NotificationRecipient,
  ): Promise<void> {
    if (!this.enabled) {
      throw new Error('SMS_DISABLED');
    }
    if (!this.accountSid || !this.authToken || !this.from) {
      throw new Error('SMS_PROVIDER_NOT_CONFIGURED');
    }
    if (!recipient.to) {
      throw new Error('SMS_RECIPIENT_MISSING');
    }

    const body = this.buildBody(notification);
    const client = twilio(this.accountSid, this.authToken);
    await client.messages.create({ from: this.from, to: recipient.to, body });
  }

  private buildBody(n: SendNotificationRequest): string {
    const text = `[${n.severity}] ${n.title}: ${n.message}`;
    return text.slice(0, 320);
  }
}
