import { BrevoClient } from '@getbrevo/brevo';

import type {
  NotificationRecipient,
  SendNotificationRequest,
} from '../../domain/notification-message.js';
import type { NotificationSender } from '../../ports/notification-sender.port.js';

export class BrevoEmailNotificationSender implements NotificationSender {
  constructor(
    private readonly enabled: boolean,
    private readonly apiKey: string,
    private readonly fromName: string,
    private readonly fromAddress: string,
  ) {}

  supports(recipient: NotificationRecipient): boolean {
    return recipient.channel === 'EMAIL';
  }

  async send(
    notification: SendNotificationRequest,
    recipient: NotificationRecipient,
  ): Promise<void> {
    if (!this.enabled) {
      throw new Error('EMAIL_DISABLED');
    }
    if (!this.apiKey) {
      throw new Error('EMAIL_PROVIDER_NOT_CONFIGURED');
    }
    if (!recipient.to) {
      throw new Error('EMAIL_RECIPIENT_MISSING');
    }

    const client = new BrevoClient({ apiKey: this.apiKey });
    await client.transactionalEmails.sendTransacEmail({
      sender: { name: this.fromName, email: this.fromAddress },
      to: [{ email: recipient.to }],
      subject: `[${notification.severity}] ${notification.title}`,
      htmlContent: this.buildHtml(notification),
      textContent: this.buildText(notification),
    });
  }

  private buildHtml(n: SendNotificationRequest): string {
    return [
      `<h2>[${n.severity}] ${n.title}</h2>`,
      `<p>${n.message}</p>`,
      n.tenantCode ? `<p><strong>Tenant:</strong> ${n.tenantCode}</p>` : '',
      `<p><strong>Event:</strong> ${n.eventId}</p>`,
    ]
      .filter(Boolean)
      .join('\n');
  }

  private buildText(n: SendNotificationRequest): string {
    const lines = [`[${n.severity}] ${n.title}`, '', n.message];
    if (n.tenantCode) lines.push(`Tenant: ${n.tenantCode}`);
    lines.push(`Event: ${n.eventId}`);
    return lines.join('\n');
  }
}
