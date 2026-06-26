import { beforeEach, describe, expect, it, vi } from 'vitest';

import { TwilioSmsMessageSender } from '../src/modules/messages/adapters/sms/twilio-sms-message.sender.js';

const createMessage = vi.fn();

vi.mock('twilio', () => ({
  default: vi.fn(() => ({
    messages: {
      create: createMessage,
    },
  })),
}));

const baseMessage = {
  eventId: 'evt-sms-001',
  severity: 'INFO' as const,
  title: 'Provider test',
  message: 'Hello from edge',
  recipients: [],
};

describe('TwilioSmsMessageSender', () => {
  beforeEach(() => {
    createMessage.mockReset();
    createMessage.mockResolvedValue({});
  });

  it('sends SMS with the configured SMS sender', async () => {
    const sender = new TwilioSmsMessageSender(true, 'sid', 'token', '+15145550000');

    await sender.send(baseMessage, { channel: 'SMS', to: '+50912345678' });

    expect(createMessage).toHaveBeenCalledWith({
      from: '+15145550000',
      to: '+50912345678',
      body: '[INFO] Provider test: Hello from edge',
    });
  });

  it('sends WhatsApp with whatsapp-prefixed sender and recipient', async () => {
    const sender = new TwilioSmsMessageSender(
      true,
      'sid',
      'token',
      '+15145550000',
      '+14155238886',
    );

    await sender.send(baseMessage, { channel: 'WHATSAPP', to: '+50912345678' });

    expect(createMessage).toHaveBeenCalledWith({
      from: 'whatsapp:+14155238886',
      to: 'whatsapp:+50912345678',
      body: '[INFO] Provider test: Hello from edge',
    });
  });
});
