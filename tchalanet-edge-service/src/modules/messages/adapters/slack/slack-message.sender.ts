import { IncomingWebhook } from '@slack/webhook';

import type { MessageRecipient, SendMessageRequest } from '../../domain/message.js';
import type { MessageSender } from '../../ports/message-sender.port.js';

export class SlackMessageSender implements MessageSender {
  constructor(
    private readonly enabled: boolean,
    private readonly webhooks: Record<string, string | undefined>,
  ) {}

  supports(recipient: MessageRecipient): boolean {
    return recipient.channel === 'SLACK';
  }

  async send(message: SendMessageRequest, recipient: MessageRecipient): Promise<void> {
    if (!this.enabled) {
      throw new Error('SLACK_DISABLED');
    }

    const key = recipient.channelKey ?? 'ops-alerts';
    const url = this.webhooks[key];
    if (!url) {
      throw new Error(`SLACK_WEBHOOK_NOT_CONFIGURED:${key}`);
    }

    const text = this.formatMessage(message);
    const webhook = new IncomingWebhook(url);
    await webhook.send({ text });
  }

  private formatMessage(message: SendMessageRequest): string {
    const lines = [`[${message.severity}] ${message.title}`, '', message.message];
    if (message.tenantCode) lines.push(`Tenant: ${message.tenantCode}`);
    lines.push(`Event: ${message.eventId}`);
    return lines.join('\n');
  }
}
