import { BrevoClient } from '@getbrevo/brevo';

import type { MessageRecipient, SendMessageRequest } from '../../domain/message.js';
import type { MessageSender } from '../../ports/message-sender.port.js';

export class BrevoEmailMessageSender implements MessageSender {
  constructor(
    private readonly enabled: boolean,
    private readonly apiKey: string,
    private readonly fromName: string,
    private readonly fromAddress: string,
  ) {}

  supports(recipient: MessageRecipient): boolean {
    return recipient.channel === 'EMAIL';
  }

  async send(message: SendMessageRequest, recipient: MessageRecipient): Promise<void> {
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
      subject: `[${message.severity}] ${message.title}`,
      htmlContent: this.buildHtml(message),
      textContent: this.buildText(message),
    });
  }

  private buildHtml(message: SendMessageRequest): string {
    return [
      `<h2>[${message.severity}] ${message.title}</h2>`,
      `<p>${message.message}</p>`,
      message.tenantCode ? `<p><strong>Tenant:</strong> ${message.tenantCode}</p>` : '',
      `<p><strong>Event:</strong> ${message.eventId}</p>`,
    ]
      .filter(Boolean)
      .join('\n');
  }

  private buildText(message: SendMessageRequest): string {
    const lines = [`[${message.severity}] ${message.title}`, '', message.message];
    if (message.tenantCode) lines.push(`Tenant: ${message.tenantCode}`);
    lines.push(`Event: ${message.eventId}`);
    return lines.join('\n');
  }
}
