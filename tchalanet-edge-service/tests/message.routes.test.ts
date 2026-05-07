import { createHmac } from 'node:crypto';
import Fastify from 'fastify';
import { afterAll, describe, expect, it } from 'vitest';

import { SendMessageService } from '../src/modules/messages/application/send-message.service.js';
import { messageRoutes } from '../src/modules/messages/http/message.routes.js';
import type { MessageSender } from '../src/modules/messages/ports/message-sender.port.js';
import { errorHandlerPlugin } from '../src/plugins/error-handler.plugin.js';

const hmacSecret = 'test-secret';

function buildTestApp(senders: MessageSender[] = []) {
  const app = Fastify({ logger: false });
  const service = new SendMessageService(senders);
  app.register(errorHandlerPlugin);
  app.register(messageRoutes(service, { secret: hmacSecret }));
  return app;
}

const validBody = {
  eventId: 'evt-route-001',
  severity: 'INFO',
  title: 'Route test',
  message: 'Testing the route',
  recipients: [{ channel: 'SLACK', channelKey: 'batch-draws' }],
};

function signedPayload(body: unknown, overrides: Record<string, string> = {}) {
  const payload = JSON.stringify(body);
  const timestamp = overrides['x-tch-timestamp'] ?? new Date().toISOString();
  const signature =
    overrides['x-tch-signature'] ??
    `sha256=${createHmac('sha256', hmacSecret)
      .update(timestamp)
      .update('.')
      .update(payload)
      .digest('hex')}`;

  return {
    payload,
    headers: {
      'content-type': 'application/json',
      'x-request-id': 'evt-route-001',
      'idempotency-key': 'evt-route-001',
      'x-tch-timestamp': timestamp,
      'x-tch-signature': signature,
      ...overrides,
    },
  };
}

describe('POST /internal/messages/send', () => {
  const app = buildTestApp();
  afterAll(() => app.close());

  it('returns 202 with valid body and no sender (delivery rejected gracefully)', async () => {
    const res = await app.inject({
      method: 'POST',
      url: '/internal/messages/send',
      ...signedPayload(validBody),
    });
    expect(res.statusCode).toBe(202);
    const body = res.json();
    expect(body.eventId).toBe('evt-route-001');
    expect(body.deliveries).toHaveLength(1);
    expect(body.deliveries[0].accepted).toBe(false);
    expect(body.deliveries[0].reason).toBe('NO_SENDER_CONFIGURED');
  });

  it('returns 202 with accepted=true when sender succeeds', async () => {
    const fakeSender: MessageSender = {
      supports: () => true,
      send: () => Promise.resolve(),
    };
    const appWithSender = buildTestApp([fakeSender]);
    afterAll(() => appWithSender.close());

    const res = await appWithSender.inject({
      method: 'POST',
      url: '/internal/messages/send',
      ...signedPayload(validBody),
    });
    expect(res.statusCode).toBe(202);
    expect(res.json().accepted).toBe(true);
  });

  it('returns 400 when body is invalid (missing required fields)', async () => {
    const res = await app.inject({
      method: 'POST',
      url: '/internal/messages/send',
      ...signedPayload({ message: 'incomplete' }),
    });
    expect(res.statusCode).toBe(400);
  });

  it('returns 400 when severity is not a valid enum value', async () => {
    const res = await app.inject({
      method: 'POST',
      url: '/internal/messages/send',
      ...signedPayload({ ...validBody, severity: 'UNKNOWN' }),
    });
    expect(res.statusCode).toBe(400);
  });

  it('returns 400 when recipients is empty', async () => {
    const res = await app.inject({
      method: 'POST',
      url: '/internal/messages/send',
      ...signedPayload({ ...validBody, recipients: [] }),
    });
    expect(res.statusCode).toBe(400);
  });

  it('returns 401 when HMAC headers are missing', async () => {
    const res = await app.inject({
      method: 'POST',
      url: '/internal/messages/send',
      payload: validBody,
    });

    expect(res.statusCode).toBe(401);
  });

  it('returns 401 when HMAC signature is invalid', async () => {
    const res = await app.inject({
      method: 'POST',
      url: '/internal/messages/send',
      ...signedPayload(validBody, { 'x-tch-signature': 'sha256=invalid' }),
    });

    expect(res.statusCode).toBe(401);
  });

  it('returns 401 when HMAC timestamp is expired', async () => {
    const res = await app.inject({
      method: 'POST',
      url: '/internal/messages/send',
      ...signedPayload(validBody, { 'x-tch-timestamp': '2020-01-01T00:00:00Z' }),
    });

    expect(res.statusCode).toBe(401);
  });

  it('does not register the legacy notification endpoint', async () => {
    const res = await app.inject({
      method: 'POST',
      url: '/internal/notifications/send',
      ...signedPayload(validBody),
    });

    expect(res.statusCode).toBe(404);
  });
});
