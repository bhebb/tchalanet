import { resolveSlotKeys, slotTypeKey } from './public-results.utils';

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
