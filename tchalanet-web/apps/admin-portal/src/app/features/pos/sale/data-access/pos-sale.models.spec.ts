import { describe, expect, it } from 'vitest';

import { isPosDrawAvailableNow } from './pos-sale.models';

describe('isPosDrawAvailableNow', () => {
  const now = Date.parse('2026-08-01T14:00:00Z');

  it('accepts an open draw before its cutoff', () => {
    expect(
      isPosDrawAvailableNow(
        { status: 'OPEN', cutoffAt: '2026-08-01T14:30:00Z' },
        now,
      ),
    ).toBe(true);
  });

  it('rejects closed or expired draws', () => {
    expect(
      isPosDrawAvailableNow(
        { status: 'CLOSED', cutoffAt: '2026-08-01T14:30:00Z' },
        now,
      ),
    ).toBe(false);
    expect(
      isPosDrawAvailableNow(
        { status: 'OPEN', cutoffAt: '2026-08-01T13:59:59Z' },
        now,
      ),
    ).toBe(false);
  });
});
