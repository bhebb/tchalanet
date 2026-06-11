import type { ProviderKey, SlotTypeKey } from './public-results.model';

export const SLOTS_BY_PROVIDER: Readonly<Record<Exclude<ProviderKey, 'all'>, readonly string[]>> = {
  ny: ['NY_MID', 'NY_EVE'],
  fl: ['FL_MID', 'FL_EVE'],
  ga: ['GA_MID', 'GA_EVE', 'GA_LATE'],
  tx: ['TX_1000', 'TX_1227', 'TX_1800', 'TX_2212'],
};

export const SLOT_TYPE_SETS: Readonly<Record<Exclude<SlotTypeKey, 'all'>, ReadonlySet<string>>> = {
  mid: new Set(['NY_MID', 'FL_MID', 'GA_MID', 'TX_1000', 'TX_1227']),
  eve: new Set(['NY_EVE', 'FL_EVE', 'GA_EVE', 'TX_1800', 'TX_2212']),
  late: new Set(['GA_LATE']),
};
