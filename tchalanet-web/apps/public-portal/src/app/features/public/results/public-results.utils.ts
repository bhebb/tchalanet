import type { ProviderKey, SlotTypeKey } from './public-results.model';

export interface PublicResultSlotFilterSource {
  readonly slotKey: string;
  readonly provider?: string | null;
  readonly drawTime?: string | null;
}

/** Returns the resolved slotKeys to send as a query param, or undefined = no filter. */
export function resolveSlotKeys(
  slots: readonly PublicResultSlotFilterSource[],
  provider: ProviderKey,
  slotType: SlotTypeKey,
): readonly string[] | undefined {
  const activeSlots = slots.filter(slot => slot.slotKey);
  const providerFiltered =
    provider === 'all'
      ? activeSlots
      : activeSlots.filter(slot => providerKey(slot.provider) === provider);

  const filtered =
    slotType === 'all'
      ? providerFiltered
      : providerFiltered.filter(slot => slotTypeKey(slot) === slotType);

  if (provider === 'all' && slotType === 'all') {
    return undefined;
  }
  return filtered.map(slot => slot.slotKey);
}

export function providerKey(provider: string | null | undefined): string {
  return (provider ?? '').trim().toLowerCase();
}

export function providerLabel(provider: string | null | undefined): string {
  const value = (provider ?? '').trim();
  if (!value) return 'Provider';
  return value.toUpperCase();
}

export function slotTypeKey(slot: PublicResultSlotFilterSource): Exclude<SlotTypeKey, 'all'> {
  const key = slot.slotKey.toUpperCase();
  if (key.includes('LATE')) return 'late';
  if (key.includes('EVE') || key.includes('EVENING')) return 'eve';

  const hour = Number((slot.drawTime ?? '').slice(0, 2));
  if (Number.isFinite(hour)) {
    if (hour >= 20) return 'late';
    if (hour >= 16) return 'eve';
  }

  return 'mid';
}
