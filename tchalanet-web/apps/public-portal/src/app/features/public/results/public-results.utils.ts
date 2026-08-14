import {
  consoleDrawResultSummaryViewModel,
  consoleResultSlotIdentity,
  type ConsoleDrawSlotIdentity,
} from '@tch/web/console';
import type { ProviderKey, SlotTypeKey } from './public-results.model';

export interface PublicResultSlotFilterSource {
  readonly slotKey: string;
  readonly provider?: string | null;
  readonly drawTime?: string | null;
}

export interface PublicResultIdentitySource {
  readonly slotKey: string;
  readonly provider?: string | null;
  readonly drawChannelLabel?: string | null;
  readonly resultDate?: string | null;
  readonly drawTime?: string | null;
  readonly timezone?: string | null;
  readonly numbers?: readonly (string | number)[];
}

export interface PublicResultIdentityView {
  readonly identity: ConsoleDrawSlotIdentity;
  readonly dateTimeLabel: string;
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
  const identity = consoleResultSlotIdentity({ providerCode: provider });
  return identity.providerLongLabel ?? identity.providerShortLabel ?? 'Provider';
}

export function publicResultIdentity(source: PublicResultIdentitySource): PublicResultIdentityView {
  const time = hhmm(source.drawTime);
  const summary = consoleDrawResultSummaryViewModel({
    identityInput: {
      slotKey: source.slotKey,
      channelCode: source.slotKey,
      providerCode: source.provider,
      channelName: source.drawChannelLabel,
      officialDateLabel: source.resultDate,
      officialTimeLabel: time,
      officialTimezoneLabel: source.timezone,
    },
    numbers: source.numbers ?? [],
  });

  return {
    identity: summary.identity,
    dateTimeLabel: [formatResultDate(source.resultDate), time].filter(Boolean).join(' · '),
  };
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

/** Formats an ISO date string (YYYY-MM-DD) for display in French. */
export function formatResultDate(iso: string | null | undefined): string {
  if (!iso) return '';
  const d = new Date(`${iso}T12:00:00`);
  if (isNaN(d.getTime())) return iso;
  return new Intl.DateTimeFormat('fr', { day: 'numeric', month: 'short', year: 'numeric' }).format(d);
}

/** Returns a CSS-safe slot category token for color theming. */
export function slotColorToken(slotKey: string): 'pick3' | 'pick4' | 'borlette' {
  const key = slotKey.toLowerCase();
  if (key.includes('pick4') || key.includes('cash4') || key.includes('daily4') || key.includes('win4')) return 'pick4';
  if (key.includes('pick3') || key.includes('cash3') || key.includes('daily3') || key.includes('numbers')) return 'pick3';
  return 'borlette';
}

function hhmm(time: string | null | undefined): string {
  if (!time) return '';
  return time.length > 5 ? time.substring(0, 5) : time;
}
