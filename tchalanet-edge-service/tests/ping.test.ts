import { afterAll,describe, expect, it } from 'vitest';

import { buildApp } from '../src/app.js';

describe('ping routes', () => {
  const app = buildApp();
  afterAll(() => app.close());

  it('GET /ping returns ok', async () => {
    const res = await app.inject({ method: 'GET', url: '/ping' });
    expect(res.statusCode).toBe(200);
    expect(res.json()).toEqual({ ok: true, service: 'tchalanet-edge-service' });
  });

  it('GET /health returns UP', async () => {
    const res = await app.inject({ method: 'GET', url: '/health' });
    expect(res.statusCode).toBe(200);
    expect(res.json()).toEqual({ status: 'UP', service: 'tchalanet-edge-service' });
  });

  it('GET /ready returns READY', async () => {
    const res = await app.inject({ method: 'GET', url: '/ready' });
    expect(res.statusCode).toBe(200);
    expect(res.json()).toEqual({ status: 'READY', service: 'tchalanet-edge-service' });
  });
});
