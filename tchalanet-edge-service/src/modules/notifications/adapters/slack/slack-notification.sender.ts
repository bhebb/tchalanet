import { IncomingWebhook } from '@slack/webhook';

import type {
  NotificationRecipient,
  SendNotificationRequest,
} from '../../domain/notification-message.js';
import type { NotificationSender } from '../../ports/notification-sender.port.js';

export class SlackNotificationSender implements NotificationSender {
  constructor(
    private readonly enabled: boolean,
    private readonly webhooks: Record<string, string | undefined>,
  ) {}

  supports(recipient: NotificationRecipient): boolean {
    return recipient.channel === 'SLACK';
  }

  async send(
    notification: SendNotificationRequest,
    recipient: NotificationRecipient,
  ): Promise<void> {
    if (!this.enabled) {
      throw new Error('SLACK_DISABLED');
    }

    const key = recipient.channelKey ?? 'ops-alerts';
    const url = this.webhooks[key];
    if (!url) {
      throw new Error(`SLACK_WEBHOOK_NOT_CONFIGURED:${key}`);
    }

    const text = this.formatMessage(notification);
    const webhook = new IncomingWebhook(url);
    await webhook.send({ text });
  }

  private formatMessage(n: SendNotificationRequest): string {
    const lines = [`[${n.severity}] ${n.title}`, '', n.message];
    if (n.tenantCode) lines.push(`Tenant: ${n.tenantCode}`);
    lines.push(`Event: ${n.eventId}`);
    return lines.join('\n');
  }
}
