import { SLOTS_BY_PROVIDER, SLOT_TYPE_SETS } from './public-results.data';
import type { ProviderKey, SlotTypeKey } from './public-results.model';

/** Returns the resolved slotKeys to send as a query param, or undefined = no filter. */
export function resolveSlotKeys(
  provider: ProviderKey,
  slotType: SlotTypeKey,
): readonly string[] | undefined {
  const base: readonly string[] =
    provider === 'all' ? Object.values(SLOTS_BY_PROVIDER).flat() : SLOTS_BY_PROVIDER[provider];

  if (slotType === 'all') {
    return provider === 'all' ? undefined : base;
  }

  const typeSet = SLOT_TYPE_SETS[slotType];
  return base.filter(k => typeSet.has(k));
}
