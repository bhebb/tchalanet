import { describe, expect, it } from 'vitest';

import { SendMessageService } from '../src/modules/messages/application/send-message.service.js';
import type {
  MessageRecipient,
  SendMessageRequest,
} from '../src/modules/messages/domain/message.js';
import type { MessageSender } from '../src/modules/messages/ports/message-sender.port.js';

const baseRequest: SendMessageRequest = {
  eventId: 'evt-test-001',
  severity: 'INFO',
  title: 'Test',
  message: 'Test message',
  recipients: [],
};

const slackRecipient: MessageRecipient = { channel: 'SLACK', channelKey: 'batch-draws' };

function makeFakeSender(channel: string, shouldFail = false): MessageSender {
  return {
    supports: r => r.channel === channel,
    send: async () => {
      if (shouldFail) throw new Error('FAKE_FAILURE');
    },
  };
}

describe('SendMessageService', () => {
  it('returns accepted=true when sender succeeds', async () => {
    const service = new SendMessageService([makeFakeSender('SLACK')]);
    const result = await service.send({ ...baseRequest, recipients: [slackRecipient] });

    expect(result.accepted).toBe(true);
    expect(result.deliveries[0].accepted).toBe(true);
  });

  it('returns accepted=false with NO_SENDER_CONFIGURED when no sender matches', async () => {
    const service = new SendMessageService([]);
    const result = await service.send({ ...baseRequest, recipients: [slackRecipient] });

    expect(result.accepted).toBe(false);
    expect(result.deliveries[0].accepted).toBe(false);
    expect(result.deliveries[0].reason).toBe('NO_SENDER_CONFIGURED');
  });

  it('does not throw when one recipient fails — partial accepted', async () => {
    const emailRecipient: MessageRecipient = { channel: 'EMAIL', to: 'x@example.com' };
    const service = new SendMessageService([
      makeFakeSender('SLACK'),
      makeFakeSender('EMAIL', true),
    ]);
    const result = await service.send({
      ...baseRequest,
      recipients: [slackRecipient, emailRecipient],
    });

    expect(result.accepted).toBe(true);
    expect(result.deliveries[0].accepted).toBe(true);
    expect(result.deliveries[1].accepted).toBe(false);
    expect(result.deliveries[1].reason).toBe('FAKE_FAILURE');
  });

  it('routes to-based recipients for email, SMS, and WhatsApp', async () => {
    const service = new SendMessageService([
      makeFakeSender('EMAIL'),
      makeFakeSender('SMS'),
      makeFakeSender('WHATSAPP'),
    ]);
    const result = await service.send({
      ...baseRequest,
      recipients: [
        { channel: 'EMAIL', to: 'x@example.com' },
        { channel: 'SMS', to: '+15145550100' },
        { channel: 'WHATSAPP', to: '+15145550101' },
      ],
    });

    expect(result.accepted).toBe(true);
    expect(result.deliveries.every(d => d.accepted)).toBe(true);
    expect(result.deliveries.map(d => d.to)).toEqual([
      'x@example.com',
      '+15145550100',
      '+15145550101',
    ]);
  });
});
