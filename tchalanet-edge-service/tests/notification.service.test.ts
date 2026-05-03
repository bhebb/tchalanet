import { describe, expect,it } from 'vitest';

import { SendNotificationService } from '../src/modules/notifications/application/send-notification.service.js';
import type {
  NotificationRecipient,
  SendNotificationRequest,
} from '../src/modules/notifications/domain/notification-message.js';
import type { NotificationSender } from '../src/modules/notifications/ports/notification-sender.port.js';

const baseRequest: SendNotificationRequest = {
  eventId: 'evt-test-001',
  severity: 'INFO',
  title: 'Test',
  message: 'Test message',
  recipients: [],
};

const slackRecipient: NotificationRecipient = { channel: 'SLACK', channelKey: 'batch-draws' };

function makeFakeSender(channel: string, shouldFail = false): NotificationSender {
  return {
    supports: r => r.channel === channel,
    send: async () => {
      if (shouldFail) throw new Error('FAKE_FAILURE');
    },
  };
}

describe('SendNotificationService', () => {
  it('returns accepted=true when sender succeeds', async () => {
    const service = new SendNotificationService([makeFakeSender('SLACK')]);
    const result = await service.send({ ...baseRequest, recipients: [slackRecipient] });

    expect(result.accepted).toBe(true);
    expect(result.deliveries[0].accepted).toBe(true);
  });

  it('returns accepted=false with NO_SENDER_CONFIGURED when no sender matches', async () => {
    const service = new SendNotificationService([]);
    const result = await service.send({ ...baseRequest, recipients: [slackRecipient] });

    expect(result.accepted).toBe(false);
    expect(result.deliveries[0].accepted).toBe(false);
    expect(result.deliveries[0].reason).toBe('NO_SENDER_CONFIGURED');
  });

  it('does not throw when one recipient fails — partial accepted', async () => {
    const emailRecipient: NotificationRecipient = { channel: 'EMAIL', to: 'x@example.com' };
    const service = new SendNotificationService([
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
});
