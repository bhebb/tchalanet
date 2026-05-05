import Fastify from 'fastify';
import { afterAll,describe, expect, it } from 'vitest';

import { SendNotificationService } from '../src/modules/notifications/application/send-notification.service.js';
import { notificationRoutes } from '../src/modules/notifications/http/notification.routes.js';
import type { NotificationSender } from '../src/modules/notifications/ports/notification-sender.port.js';
import { errorHandlerPlugin } from '../src/plugins/error-handler.plugin.js';

function buildTestApp(senders: NotificationSender[] = []) {
  const app = Fastify({ logger: false });
  const service = new SendNotificationService(senders);
  app.register(errorHandlerPlugin);
  app.register(notificationRoutes(service));
  return app;
}

const validBody = {
  eventId: 'evt-route-001',
  severity: 'INFO',
  title: 'Route test',
  message: 'Testing the route',
  recipients: [{ channel: 'SLACK', channelKey: 'batch-draws' }],
};

describe('POST /internal/notifications/send', () => {
  const app = buildTestApp();
  afterAll(() => app.close());

  it('returns 202 with valid body and no sender (delivery rejected gracefully)', async () => {
    const res = await app.inject({
      method: 'POST',
      url: '/internal/notifications/send',
      payload: validBody,
    });
    expect(res.statusCode).toBe(202);
    const body = res.json();
    expect(body.eventId).toBe('evt-route-001');
    expect(body.deliveries).toHaveLength(1);
    expect(body.deliveries[0].accepted).toBe(false);
    expect(body.deliveries[0].reason).toBe('NO_SENDER_CONFIGURED');
  });

  it('returns 202 with accepted=true when sender succeeds', async () => {
    const fakeSender: NotificationSender = {
      supports: () => true,
      send: () => Promise.resolve(),
    };
    const appWithSender = buildTestApp([fakeSender]);
    afterAll(() => appWithSender.close());

    const res = await appWithSender.inject({
      method: 'POST',
      url: '/internal/notifications/send',
      payload: validBody,
    });
    expect(res.statusCode).toBe(202);
    expect(res.json().accepted).toBe(true);
  });

  it('returns 400 when body is invalid (missing required fields)', async () => {
    const res = await app.inject({
      method: 'POST',
      url: '/internal/notifications/send',
      payload: { message: 'incomplete' },
    });
    expect(res.statusCode).toBe(400);
  });

  it('returns 400 when severity is not a valid enum value', async () => {
    const res = await app.inject({
      method: 'POST',
      url: '/internal/notifications/send',
      payload: { ...validBody, severity: 'UNKNOWN' },
    });
    expect(res.statusCode).toBe(400);
  });

  it('returns 400 when recipients is empty', async () => {
    const res = await app.inject({
      method: 'POST',
      url: '/internal/notifications/send',
      payload: { ...validBody, recipients: [] },
    });
    expect(res.statusCode).toBe(400);
  });
});
