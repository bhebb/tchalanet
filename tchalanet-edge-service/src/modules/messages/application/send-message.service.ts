import type {
  MessageDeliveryResult,
  MessageRecipient,
  SendMessageRequest,
  SendMessageResponse,
} from '../domain/message.js';
import type { MessageSender } from '../ports/message-sender.port.js';

export class SendMessageService {
  constructor(private readonly senders: MessageSender[]) {}

  async send(request: SendMessageRequest): Promise<SendMessageResponse> {
    const deliveries: MessageDeliveryResult[] = [];

    for (const recipient of request.recipients) {
      const result = await this.deliver(request, recipient);
      deliveries.push(result);
    }

    return {
      accepted: deliveries.some(d => d.accepted),
      eventId: request.eventId,
      deliveries,
    };
  }

  private async deliver(
    request: SendMessageRequest,
    recipient: MessageRecipient,
  ): Promise<MessageDeliveryResult> {
    const base = {
      channel: recipient.channel,
      to: recipient.to,
      channelKey: recipient.channelKey,
    };

    const sender = this.senders.find(s => s.supports(recipient));
    if (!sender) {
      return { ...base, accepted: false, reason: 'NO_SENDER_CONFIGURED' };
    }

    try {
      await sender.send(request, recipient);
      return { ...base, accepted: true };
    } catch (err) {
      const reason = err instanceof Error ? err.message : 'UNKNOWN_ERROR';
      return { ...base, accepted: false, reason };
    }
  }
}
