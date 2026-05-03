import type {
  NotificationDeliveryResult,
  NotificationRecipient,
  SendNotificationRequest,
  SendNotificationResponse,
} from '../domain/notification-message.js';
import type { NotificationSender } from '../ports/notification-sender.port.js';

export class SendNotificationService {
  constructor(private readonly senders: NotificationSender[]) {}

  async send(request: SendNotificationRequest): Promise<SendNotificationResponse> {
    const deliveries: NotificationDeliveryResult[] = [];

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
    request: SendNotificationRequest,
    recipient: NotificationRecipient,
  ): Promise<NotificationDeliveryResult> {
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
