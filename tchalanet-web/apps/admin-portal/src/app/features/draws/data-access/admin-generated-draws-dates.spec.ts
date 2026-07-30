import { describe, expect, it } from 'vitest';

import { shiftIsoDate, tenantTodayIsoDate } from './admin-generated-draws.models';

/**
 * `drawDate` is the channel-local commercial date, so the from/to filters must be expressed on
 * the tenant's calendar. Deriving them from `toISOString()` silently queried the wrong day every
 * evening in Haiti — these tests pin the boundary so it cannot regress.
 */
describe('tenantTodayIsoDate', () => {
  const HAITI = 'America/Port-au-Prince';

  it('stays on the current local day when UTC has already rolled over', () => {
    // 01:30 UTC on the 31st is still 20:30 on the 30th in Haiti (UTC-5).
    const afterUtcMidnight = new Date('2026-07-31T01:30:00Z');

    expect(tenantTodayIsoDate(HAITI, afterUtcMidnight)).toBe('2026-07-30');
    expect(afterUtcMidnight.toISOString().slice(0, 10)).toBe('2026-07-31');
  });

  it('agrees with UTC during the tenant daytime', () => {
    const midday = new Date('2026-07-30T16:00:00Z');

    expect(tenantTodayIsoDate(HAITI, midday)).toBe('2026-07-30');
  });

  it('rolls over at local midnight during DST (UTC-4 in July)', () => {
    expect(tenantTodayIsoDate(HAITI, new Date('2026-07-31T03:59:00Z'))).toBe('2026-07-30');
    expect(tenantTodayIsoDate(HAITI, new Date('2026-07-31T04:01:00Z'))).toBe('2026-07-31');
  });

  it('rolls over an hour later outside DST (UTC-5 in January)', () => {
    // Haiti observes DST, so the boundary is not a fixed offset — an hour-shifted assertion
    // here is what catches a hand-rolled UTC-5 constant.
    expect(tenantTodayIsoDate(HAITI, new Date('2026-01-15T04:59:00Z'))).toBe('2026-01-14');
    expect(tenantTodayIsoDate(HAITI, new Date('2026-01-15T05:01:00Z'))).toBe('2026-01-15');
  });
});

describe('shiftIsoDate', () => {
  it('shifts forward and backward', () => {
    expect(shiftIsoDate('2026-07-30', -1)).toBe('2026-07-29');
    expect(shiftIsoDate('2026-07-30', 6)).toBe('2026-08-05');
  });

  it('crosses month and year boundaries', () => {
    expect(shiftIsoDate('2026-03-01', -1)).toBe('2026-02-28');
    expect(shiftIsoDate('2026-12-31', 1)).toBe('2027-01-01');
  });

  it('handles a leap day', () => {
    expect(shiftIsoDate('2028-02-28', 1)).toBe('2028-02-29');
  });
});
