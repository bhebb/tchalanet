import { BrevoClient } from '@getbrevo/brevo';

import type {
  MessageRecipient,
  MessageSendResult,
  SendMessageRequest,
} from '../../domain/message.js';
import type { MessageSender } from '../../ports/message-sender.port.js';

interface EmailAttachment {
  name: string;
  content: string;
}

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

  async send(
    message: SendMessageRequest,
    recipient: MessageRecipient,
  ): Promise<MessageSendResult> {
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
    const response = await client.transactionalEmails.sendTransacEmail({
      sender: { name: this.fromName, email: this.fromAddress },
      to: [{ email: recipient.to }],
      subject: `[${message.severity}] ${message.title}`,
      htmlContent: this.buildHtml(message),
      textContent: this.buildText(message),
      attachment: this.buildAttachments(message),
    });
    return { providerMessageId: this.extractMessageId(response) };
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

  private buildAttachments(message: SendMessageRequest): EmailAttachment[] | undefined {
    const raw = message.context?.['attachments'];
    if (!Array.isArray(raw)) {
      return undefined;
    }

    const attachments = raw
      .map(item => this.toAttachment(item))
      .filter((item): item is EmailAttachment => item !== undefined);
    return attachments.length > 0 ? attachments : undefined;
  }

  private toAttachment(raw: unknown): EmailAttachment | undefined {
    if (!raw || typeof raw !== 'object') {
      return undefined;
    }

    const item = raw as Record<string, unknown>;
    const filename = typeof item['filename'] === 'string' ? item['filename'].trim() : '';
    const contentBase64 =
      typeof item['contentBase64'] === 'string' ? item['contentBase64'].trim() : '';

    if (!filename || !contentBase64) {
      return undefined;
    }

    return { name: filename, content: contentBase64 };
  }

  private extractMessageId(response: unknown): string | undefined {
    if (!response || typeof response !== 'object') {
      return undefined;
    }

    const root = response as Record<string, unknown>;
    const direct = root['messageId'];
    if (typeof direct === 'string' && direct.trim()) {
      return direct.trim();
    }

    const body = root['body'];
    if (!body || typeof body !== 'object') {
      return undefined;
    }

    const nested = (body as Record<string, unknown>)['messageId'];
    return typeof nested === 'string' && nested.trim() ? nested.trim() : undefined;
  }
}
