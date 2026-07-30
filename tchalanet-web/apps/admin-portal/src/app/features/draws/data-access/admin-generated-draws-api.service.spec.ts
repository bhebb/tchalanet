import { describe, expect, it } from 'vitest';

import { applyQueryFilter } from './admin-generated-draws-api.service';
import { GeneratedDrawView } from './admin-generated-draws.models';

function draw(overrides: Partial<GeneratedDrawView>): GeneratedDrawView {
  return {
    drawId: 'draw-1',
    drawChannelId: 'channel-1',
    drawChannelCode: 'NY_EVE',
    providerCode: 'NY',
    providerLabel: 'New York',
    slotKey: 'ny-eve',
    slotLabel: 'Soir',
    label: 'New York · Soir',
    businessDate: '2026-07-29',
    scheduledAt: '2026-07-29T19:00:00Z',
    timezone: 'America/New_York',
    salesStatus: 'OPEN',
    resultStatus: 'NOT_DUE',
    resultMode: 'MANUAL',
    ...overrides,
  };
}

describe('applyQueryFilter', () => {
  it('returns every draw when the query is empty', () => {
    const draws = [draw({}), draw({ drawId: 'draw-2' })];
    expect(applyQueryFilter(draws, '')).toEqual(draws);
    expect(applyQueryFilter(draws, null)).toEqual(draws);
    expect(applyQueryFilter(draws, undefined)).toEqual(draws);
  });

  it('matches "GA-" against a channel code stored with an underscore — the case reported live', () => {
    const ga = draw({ drawId: 'ga', drawChannelCode: 'GA_LATE', providerCode: 'GA', providerLabel: 'Georgia', label: 'Georgia · Late' });
    const ny = draw({ drawId: 'ny' });
    expect(applyQueryFilter([ga, ny], 'GA-')).toEqual([ga]);
  });

  it('matches case-insensitively against the channel code', () => {
    const result = applyQueryFilter([draw({ drawChannelCode: 'NY_EVE' })], 'ny_eve');
    expect(result).toHaveLength(1);
  });

  it('matches against the human label', () => {
    const result = applyQueryFilter([draw({ label: 'New York · Soir' })], 'soir');
    expect(result).toHaveLength(1);
  });

  it('excludes draws that match nothing', () => {
    expect(applyQueryFilter([draw({})], 'texas')).toEqual([]);
  });
});
