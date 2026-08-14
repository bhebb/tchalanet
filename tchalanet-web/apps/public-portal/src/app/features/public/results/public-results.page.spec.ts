import { formatResultDate, providerLabel, publicResultIdentity, resolveSlotKeys, slotTypeKey } from './public-results.utils';

const slots = [
  { slotKey: 'NY_MID', provider: 'NY', drawTime: '12:30:00' },
  { slotKey: 'NY_EVE', provider: 'NY', drawTime: '19:30:00' },
  { slotKey: 'GA_LATE', provider: 'GA', drawTime: '22:30:00' },
  { slotKey: 'PR_NIGHT', provider: 'PR', drawTime: '21:00:00' },
] as const;

describe('resolveSlotKeys', () => {
  it('returns undefined when both filters are "all" (no query restriction)', () => {
    expect(resolveSlotKeys(slots, 'all', 'all')).toBeUndefined();
  });

  it('returns dynamic slots for a provider', () => {
    expect(resolveSlotKeys(slots, 'ny', 'all')).toEqual(['NY_MID', 'NY_EVE']);
    expect(resolveSlotKeys(slots, 'pr', 'all')).toEqual(['PR_NIGHT']);
  });

  it('cross-filters provider and slot type dynamically', () => {
    expect(resolveSlotKeys(slots, 'ny', 'mid')).toEqual(['NY_MID']);
    expect(resolveSlotKeys(slots, 'ny', 'eve')).toEqual(['NY_EVE']);
    expect(resolveSlotKeys(slots, 'ga', 'late')).toEqual(['GA_LATE']);
  });

  it('classifies new slots by draw time when slot key has no known suffix', () => {
    expect(slotTypeKey({ slotKey: 'PR_NIGHT', provider: 'PR', drawTime: '21:00:00' })).toBe('late');
  });
});

describe('public result identity', () => {
  it('derives provider and channel labels from structured slot data', () => {
    const view = publicResultIdentity({
      slotKey: 'NY_MID',
      provider: 'NY',
      resultDate: '2026-07-04',
      drawTime: '12:30:00',
      timezone: 'America/New_York',
    });

    expect(view.identity.providerName).toBe('New York');
    expect(view.identity.channelName).toBe('New York · Midday');
    expect(view.identity.providerLogoUrl).toContain('/assets/images/logo/ny_logo.png');
    expect(view.dateTimeLabel).toBe(`${formatResultDate('2026-07-04')} · 12:30`);
  });

  it('keeps legacy Haiti channel labels as fallback instead of canonical public labels', () => {
    const view = publicResultIdentity({
      slotKey: 'GA_LATE',
      provider: 'GA',
      drawChannelLabel: 'Haïti • Georgia • Late',
      resultDate: '2026-07-04',
      drawTime: '22:30:00',
      numbers: ['242', '86', '02'],
    });

    expect(view.identity.channelName).toBe('Georgia · Late');
    expect(view.identity.providerName).toBe('Georgia');
    expect(view.identity.providerLogoUrl).toContain('/assets/images/logo/ga_logo.png');
  });

  it('uses long provider labels for filters', () => {
    expect(providerLabel('GA')).toBe('Georgia');
  });
});
