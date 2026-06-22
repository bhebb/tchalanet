import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest';

import { buildCopyText } from './copy-error-details';

describe('buildCopyText', () => {
  const baseItem = {
    title: 'Erreur serveur',
    message: 'Le service est indisponible',
    status: 503,
    source: '/api/sales',
    requestId: 'tch_req_abc123',
    traceId: 'aabbccdd11223344aabbccdd11223344',
    spanId: '1122334455667788',
  };

  it('includes requestId, traceId, spanId, route, time', () => {
    const text = buildCopyText(baseItem);
    expect(text).toContain('requestId=tch_req_abc123');
    expect(text).toContain('traceId=aabbccdd11223344aabbccdd11223344');
    expect(text).toContain('spanId=1122334455667788');
    expect(text).toContain('route=/api/sales');
    expect(text).toContain('time=');
  });

  it('omits absent trace fields', () => {
    const text = buildCopyText({ title: 'Err', message: 'Err', status: 500 });
    expect(text).not.toContain('requestId=');
    expect(text).not.toContain('traceId=');
    expect(text).not.toContain('spanId=');
    expect(text).not.toContain('route=');
  });

  it('time field is ISO-8601', () => {
    const before = Date.now();
    const text = buildCopyText(baseItem);
    const after = Date.now();
    const match = text.match(/time=([^\s]+)/);
    expect(match).not.toBeNull();
    const t = new Date(match![1]).getTime();
    expect(t).toBeGreaterThanOrEqual(before);
    expect(t).toBeLessThanOrEqual(after);
  });

  it('does not include tokens, email or PII', () => {
    const text = buildCopyText({
      ...baseItem,
      title: 'auth.failure',
      message: 'Accès refusé',
    });
    expect(text).not.toMatch(/bearer|Authorization|password|email|@/i);
  });
});
