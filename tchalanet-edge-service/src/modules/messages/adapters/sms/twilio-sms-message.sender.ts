import twilio from 'twilio';

import type { MessageRecipient, SendMessageRequest } from '../../domain/message.js';
import type { MessageSender } from '../../ports/message-sender.port.js';

export class TwilioSmsMessageSender implements MessageSender {
  constructor(
    private readonly enabled: boolean,
    private readonly accountSid: string,
    private readonly authToken: string,
    private readonly from: string,
  ) {}

  supports(recipient: MessageRecipient): boolean {
    return recipient.channel === 'SMS';
  }

  async send(message: SendMessageRequest, recipient: MessageRecipient): Promise<void> {
    if (!this.enabled) {
      throw new Error('SMS_DISABLED');
    }
    if (!this.accountSid || !this.authToken || !this.from) {
      throw new Error('SMS_PROVIDER_NOT_CONFIGURED');
    }
    if (!recipient.to) {
      throw new Error('SMS_RECIPIENT_MISSING');
    }

    const body = this.buildBody(message);
    const client = twilio(this.accountSid, this.authToken);
    await client.messages.create({ from: this.from, to: recipient.to, body });
  }

  private buildBody(message: SendMessageRequest): string {
    const text = `[${message.severity}] ${message.title}: ${message.message}`;
    return text.slice(0, 320);
  }
}
