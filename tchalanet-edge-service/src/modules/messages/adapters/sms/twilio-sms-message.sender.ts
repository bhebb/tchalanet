import twilio from 'twilio';

import type { MessageRecipient, SendMessageRequest } from '../../domain/message.js';
import type { MessageSender } from '../../ports/message-sender.port.js';

export class TwilioSmsMessageSender implements MessageSender {
  constructor(
    private readonly enabled: boolean,
    private readonly accountSid: string,
    private readonly authToken: string,
    private readonly from: string,
    private readonly whatsappFrom = '',
  ) {}

  supports(recipient: MessageRecipient): boolean {
    return recipient.channel === 'SMS' || recipient.channel === 'WHATSAPP';
  }

  async send(message: SendMessageRequest, recipient: MessageRecipient): Promise<void> {
    if (!this.enabled) {
      throw new Error('SMS_DISABLED');
    }
    if (!this.accountSid || !this.authToken) {
      throw new Error('SMS_PROVIDER_NOT_CONFIGURED');
    }
    if (!recipient.to) {
      throw new Error('SMS_RECIPIENT_MISSING');
    }

    const body = this.buildBody(message);
    const client = twilio(this.accountSid, this.authToken);
    await client.messages.create({
      from: this.senderFor(recipient),
      to: this.recipientFor(recipient),
      body,
    });
  }

  private senderFor(recipient: MessageRecipient): string {
    if (recipient.channel === 'WHATSAPP') {
      const from = this.whatsappFrom || this.from;
      if (!from) {
        throw new Error('WHATSAPP_PROVIDER_NOT_CONFIGURED');
      }
      return this.ensureWhatsappAddress(from);
    }
    if (!this.from) {
      throw new Error('SMS_PROVIDER_NOT_CONFIGURED');
    }
    return this.from;
  }

  private recipientFor(recipient: MessageRecipient): string {
    if (!recipient.to) {
      throw new Error('SMS_RECIPIENT_MISSING');
    }
    return recipient.channel === 'WHATSAPP'
      ? this.ensureWhatsappAddress(recipient.to)
      : recipient.to;
  }

  private ensureWhatsappAddress(value: string): string {
    return value.startsWith('whatsapp:') ? value : `whatsapp:${value}`;
  }

  private buildBody(message: SendMessageRequest): string {
    const text = `[${message.severity}] ${message.title}: ${message.message}`;
    return text.slice(0, 320);
  }
}
