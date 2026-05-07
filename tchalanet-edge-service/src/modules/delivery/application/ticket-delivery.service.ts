import type { MessageChannel } from '../../messages/domain/message.js';
import type { MessageSender } from '../../messages/ports/message-sender.port.js';
import type {
  DeliveryChannel,
  TicketDeliveryRequest,
  TicketDeliveryResponse,
} from '../domain/delivery-request.js';

export class TicketDeliveryService {
  constructor(private readonly senders: MessageSender[]) {}

  async deliver(request: TicketDeliveryRequest): Promise<TicketDeliveryResponse> {
    const channel = request.channel;
    const recipient = { channel: channelToMessage(channel), to: request.recipient };

    const sender = this.senders.find(s => s.supports(recipient));
    if (!sender) {
      return {
        requestId: request.requestId,
        accepted: false,
        channel,
        reason: 'NO_SENDER_CONFIGURED',
      };
    }

    const message = buildMessage(request);

    try {
      await sender.send(message, recipient);
      return { requestId: request.requestId, accepted: true, channel };
    } catch (err) {
      const reason = err instanceof Error ? err.message : 'UNKNOWN_ERROR';
      return { requestId: request.requestId, accepted: false, channel, reason };
    }
  }
}

function channelToMessage(ch: DeliveryChannel): MessageChannel {
  if (ch === 'SMS' || ch === 'WHATSAPP') return 'SMS';
  return 'EMAIL';
}

function buildMessage(r: TicketDeliveryRequest) {
  const lines = (r.lines ?? [])
    .map(l => `${l.gameCode} / ${l.selection} — ${l.stake} HTG`)
    .join('\n');

  const body = [
    `Ticket: ${r.ticketCode}`,
    r.outletName ? `Point de vente: ${r.outletName}` : '',
    r.drawChannelLabel ? `Tirage: ${r.drawChannelLabel}` : '',
    r.drawWhenLabel ? `Date: ${r.drawWhenLabel}` : '',
    `Montant: ${r.totalAmount ?? ''} ${r.currency ?? ''}`.trim(),
    lines ? `\nJeux:\n${lines}` : '',
    r.includeVerificationLink && r.verificationUrl ? `\nVérifier: ${r.verificationUrl}` : '',
  ]
    .filter(Boolean)
    .join('\n');

  return {
    eventId: r.requestId,
    tenantCode: undefined,
    severity: 'INFO' as const,
    title: `Votre ticket Tchalanet — ${r.ticketCode}`,
    message: body,
    recipients: [],
    context: { ticketCode: r.ticketCode, publicCode: r.publicCode },
  };
}
