import { resolveSlotKeys } from './public-results.utils';

describe('resolveSlotKeys', () => {
  it('returns undefined when both filters are "all" (no query restriction)', () => {
    expect(resolveSlotKeys('all', 'all')).toBeUndefined();
  });

  it('returns all slots for a provider when slot type is "all"', () => {
    expect(resolveSlotKeys('ny', 'all')).toEqual(['NY_MID', 'NY_EVE']);
    expect(resolveSlotKeys('fl', 'all')).toEqual(['FL_MID', 'FL_EVE']);
    expect(resolveSlotKeys('ga', 'all')).toEqual(['GA_MID', 'GA_EVE', 'GA_LATE']);
    expect(resolveSlotKeys('tx', 'all')).toEqual(['TX_1000', 'TX_1227', 'TX_1800', 'TX_2212']);
  });

  it('cross-filters provider + mid', () => {
    expect(resolveSlotKeys('ny', 'mid')).toEqual(['NY_MID']);
    expect(resolveSlotKeys('fl', 'mid')).toEqual(['FL_MID']);
    expect(resolveSlotKeys('tx', 'mid')).toEqual(['TX_1000', 'TX_1227']);
    expect(resolveSlotKeys('ga', 'mid')).toEqual(['GA_MID']);
  });

  it('cross-filters provider + eve', () => {
    expect(resolveSlotKeys('ny', 'eve')).toEqual(['NY_EVE']);
    expect(resolveSlotKeys('fl', 'eve')).toEqual(['FL_EVE']);
    expect(resolveSlotKeys('tx', 'eve')).toEqual(['TX_1800', 'TX_2212']);
    expect(resolveSlotKeys('ga', 'eve')).toEqual(['GA_EVE']);
  });

  it('cross-filters provider + late', () => {
    expect(resolveSlotKeys('ga', 'late')).toEqual(['GA_LATE']);
  });

  it('returns empty array for impossible combinations', () => {
    // NY has no LATE slot
    expect(resolveSlotKeys('ny', 'late')).toEqual([]);
    // FL has no LATE slot
    expect(resolveSlotKeys('fl', 'late')).toEqual([]);
    // TX has no LATE slot
    expect(resolveSlotKeys('tx', 'late')).toEqual([]);
  });

  it('returns all mid slots across all providers when provider is "all"', () => {
    const result = resolveSlotKeys('all', 'mid');
    expect(result).toContain('NY_MID');
    expect(result).toContain('FL_MID');
    expect(result).toContain('GA_MID');
    expect(result).toContain('TX_1000');
    expect(result).toContain('TX_1227');
  });

  it('returns all eve slots across all providers when provider is "all"', () => {
    const result = resolveSlotKeys('all', 'eve');
    expect(result).toContain('NY_EVE');
    expect(result).toContain('FL_EVE');
    expect(result).toContain('GA_EVE');
    expect(result).toContain('TX_1800');
    expect(result).toContain('TX_2212');
  });
});
