import { beforeEach, describe, expect, it, vi } from 'vitest';

import { BrevoEmailMessageSender } from '../src/modules/messages/adapters/email/brevo-email-message.sender.js';
import type { SendMessageRequest } from '../src/modules/messages/domain/message.js';

const sendTransacEmail = vi.fn();

vi.mock('@getbrevo/brevo', () => ({
  BrevoClient: vi.fn().mockImplementation(function BrevoClientMock() {
    return {
      transactionalEmails: {
        sendTransacEmail,
      },
    };
  }),
}));

const baseMessage: SendMessageRequest = {
  eventId: 'ticket-print-001',
  severity: 'INFO',
  title: 'Ticket Tchalanet',
  message: 'Ticket receipt',
  recipients: [{ channel: 'EMAIL', to: 'buyer@example.com' }],
};

describe('BrevoEmailMessageSender', () => {
  beforeEach(() => {
    sendTransacEmail.mockReset();
    sendTransacEmail.mockResolvedValue({ messageId: '<brevo-message-id@smtp-relay.mailin.fr>' });
  });

  it('maps context attachments to Brevo transactional email attachments', async () => {
    const sender = new BrevoEmailMessageSender(
      true,
      'brevo-key',
      'Tchalanet',
      'no-reply@example.com',
    );

    const result = await sender.send(
      {
        ...baseMessage,
        context: {
          attachments: [
            {
              filename: 'ticket.pdf',
              contentType: 'application/pdf',
              contentBase64: 'JVBERi0xLjQ=',
            },
          ],
        },
      },
      { channel: 'EMAIL', to: 'buyer@example.com' },
    );

    expect(sendTransacEmail).toHaveBeenCalledWith(
      expect.objectContaining({
        attachment: [{ name: 'ticket.pdf', content: 'JVBERi0xLjQ=' }],
      }),
    );
    expect(result.providerMessageId).toBe('<brevo-message-id@smtp-relay.mailin.fr>');
  });
});
