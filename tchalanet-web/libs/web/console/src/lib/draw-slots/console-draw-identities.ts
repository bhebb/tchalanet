import {
  TCH_LOTTERY_ASSET_BASE_PATH,
  TCH_LOTTERY_LOGO_ASSET_BASE_PATH,
} from '@tch/shared-assets';

import { ConsoleDrawSlotIdentity } from './console-draw-slot-identity.models';

export interface ConsoleDrawIdentityInput {
  readonly providerCode?: string | null;
  readonly providerName?: string | null;
  readonly channelCode?: string | null;
  readonly channelName?: string | null;
  readonly channelShortName?: string | null;
  readonly slotKey?: string | null;
  readonly slotLabel?: string | null;
  readonly officialDateLabel?: string | null;
  readonly officialTimeLabel?: string | null;
  readonly officialTimezoneLabel?: string | null;
  readonly localDateLabel?: string | null;
  readonly localTimeLabel?: string | null;
  readonly localTimezoneLabel?: string | null;
  readonly fallbackTitle?: string | null;
}

export interface ConsoleDrawChannelIdentityInput {
  readonly code?: string | null;
  readonly name?: string | null;
  readonly providerCode?: string | null;
  readonly providerName?: string | null;
  readonly shortName?: string | null;
  readonly slotKey?: string | null;
}

export interface ConsoleResultSlotIdentityInput {
  readonly slotKey?: string | null;
  readonly slotLabel?: string | null;
  readonly providerCode?: string | null;
  readonly providerName?: string | null;
}

export interface ConsoleDrawChannelIdentity {
  readonly code: string | null;
  readonly shortLabel: string;
  readonly longLabel: string;
  readonly providerCode: string | null;
  readonly providerShortLabel: string | null;
  readonly providerLongLabel: string | null;
  readonly providerLogoUrl: string | null;
  readonly channelLogoUrl: string | null;
  readonly logoText: string;
}

export interface ConsoleResultSlotIdentity {
  readonly slotKey: string | null;
  readonly shortLabel: string;
  readonly longLabel: string;
  readonly providerCode: string | null;
  readonly providerShortLabel: string | null;
  readonly providerLongLabel: string | null;
  readonly providerLogoUrl: string | null;
  readonly slotLogoUrl: string | null;
  readonly logoText: string;
}

const SLOT_ASSETS: Record<string, string> = {
  'ca-pick3': 'ca_pick3.svg',
  'ca-pick4': 'ca_pick4.svg',
  'ca-daily3': 'ca_pick3.svg',
  'ca-daily4': 'ca_pick4.svg',
  'fl-pick3': 'fl_pick3.svg',
  'fl-pick4': 'fl_pick4.svg',
  'fl-middaycash3': 'fl_pick3.svg',
  'fl-play4midday': 'fl_pick4.svg',
  'ga-pick3': 'ga_pick3.svg',
  'ga-pick4': 'ga_pick4.svg',
  'ga-midday3': 'ga_pick3.svg',
  'ga-midday4': 'ga_pick4.svg',
  'il-pick3': 'il_pick3.svg',
  'il-pick4': 'il_pick4.svg',
  'il-midday3': 'il_pick3.svg',
  'il-midday4': 'il_pick4.svg',
  'mi-pick3': 'mi_pick3.svg',
  'mi-pick4': 'mi_pick4.svg',
  'mi-daily3': 'mi_pick3.svg',
  'mi-midday4': 'mi_pick4.svg',
  'mn-pick3': 'mn_pick3.svg',
  'mn-daily3-new': 'mn_pick3.svg',
  'mo-pick3': 'mo_pick3.svg',
  'mo-pick4': 'mo_pick4.svg',
  'mo-middaypick3': 'mo_pick3.svg',
  'mo-middaypick4': 'mo_pick4.svg',
  'ms-pick3': 'ms_pick3.svg',
  'ms-pick4': 'ms_pick4.svg',
  'ms-cash3': 'ms_pick3.svg',
  'ms-cash4': 'ms_pick4.svg',
  'nj-pick3': 'nj_pick3.svg',
  'nj-pick4': 'nj_pick4.svg',
  'nj-middaypick4': 'nj_pick4.svg',
  'ny-pick3': 'ny_pick3.svg',
  'ny-pick4': 'ny_pick4.svg',
  'ny-middaynumbers': 'ny_pick3.svg',
  'ny-middaywin4': 'ny_pick4.svg',
  'oh-pick3': 'oh_pick3.svg',
  'oh-pick4': 'oh_pick4.svg',
  'oh-middaypick4': 'oh_pick4.svg',
  'pa-pick3': 'pa_pick3.svg',
  'pa-pick4': 'pa_pick4.svg',
  'pa-middaypick4': 'pa_pick4.svg',
  'tn-pick3': 'tn_pick3.svg',
  'tn-pick4': 'tn_pick4.svg',
  'tn-cash3': 'tn_pick3.svg',
  'tn-morningcash4': 'tn_pick4.svg',
  'tx-pick3': 'tx_pick3.svg',
  'tx-pick4': 'tx_pick4.png',
  'tx-middaypick3': 'tx_pick3.svg',
  'tx-morningpick4': 'tx_pick4_morning.svg',
  'wi-pick3': 'wi_pick3.svg',
  'wi-pick4': 'wi_pick4.svg',
  'wi-dailypick4': 'wi_pick4.svg',
  'wi-pick3evening': 'wi_pick3.svg',
};

const CHANNEL_ASSETS: Record<string, string> = {
  pick3: 'pick3-logo.png',
  pick4: 'pick4-logo.png',
  numbers: 'logo-numbers.png.webp',
  win4: 'logo-win4.png.webp',
};

const PROVIDER_LOGO_ASSETS: Record<string, string> = {
  ca: 'calottery-logo.png',
  fl: 'florida-lottery.jpeg',
  ga: 'ga_logo.png',
  il: 'illinois-logo.svg',
  mi: 'mi_logo.webp',
  ms: 'msl-logo.svg',
  nj: 'nj_lottery_logo.png',
  ny: 'ny_logo.png',
  oh: 'oh_logo.png',
  pa: 'PALotteryBlack.svg',
  tx: 'tx_logo.png',
  wi: 'wi_logo.png',
};

const PROVIDER_LABELS: Record<string, string> = {
  CA: 'California',
  FL: 'Florida',
  GA: 'Georgia',
  IL: 'Illinois',
  MI: 'Michigan',
  MO: 'Missouri',
  MS: 'Mississippi',
  NJ: 'New Jersey',
  NY: 'New York',
  OH: 'Ohio',
  PA: 'Pennsylvania',
  TN: 'Tennessee',
  TX: 'Texas',
  WI: 'Wisconsin',
};

const SLOT_LABELS: Record<string, string> = {
  DAY: 'Day',
  EVE: 'Evening',
  EVENING: 'Evening',
  LATE: 'Late',
  MID: 'Midday',
  MIDDAY: 'Midday',
  MORNING: 'Morning',
};

export function consoleDrawIdentity(input: ConsoleDrawIdentityInput): ConsoleDrawSlotIdentity {
  const slot = consoleResultSlotIdentity(input);
  const channel = consoleDrawChannelIdentity({
    code: input.channelCode,
    name: input.channelName,
    providerCode: input.providerCode ?? slot.providerCode,
    providerName: input.providerName,
    shortName: input.channelShortName,
    slotKey: input.slotKey,
  });
  const title = firstText(input.channelShortName, channel.shortLabel, input.slotLabel, slot.shortLabel, input.fallbackTitle);

  return {
    providerCode: channel.providerCode ?? slot.providerCode,
    providerName: firstText(input.providerName, channel.providerLongLabel, slot.providerLongLabel),
    providerShortName: channel.providerShortLabel ?? slot.providerShortLabel,
    providerLogoUrl: channel.providerLogoUrl ?? slot.providerLogoUrl,
    logoText: channel.logoText || slot.logoText,
    slotKey: firstText(input.slotKey, slot.slotKey),
    slotLabel: firstText(input.slotLabel, slot.longLabel),
    channelCode: channel.code,
    channelName: firstText(channel.longLabel, input.channelName, title),
    channelShortName: title,
    localDateLabel: clean(input.localDateLabel),
    localTimeLabel: clean(input.localTimeLabel),
    localTimezoneLabel: clean(input.localTimezoneLabel),
    providerDateLabel: clean(input.officialDateLabel),
    providerTimeLabel: clean(input.officialTimeLabel),
    providerTimezoneLabel: clean(input.officialTimezoneLabel),
  };
}

export function consoleDrawChannelIdentity(input: ConsoleDrawChannelIdentityInput): ConsoleDrawChannelIdentity {
  const providerCode = providerCodeFrom(input.providerCode, input.slotKey, input.code);
  const code = clean(input.code);
  const shortLabel = firstText(input.shortName, channelLabelFromCode(code), code, input.name, providerCode, 'Tirage') ?? 'Tirage';
  const providerLongLabel = providerLabel(providerCode, input.providerName);
  const longLabel = firstText(joinLabels(providerLongLabel, shortLabel), input.name, shortLabel) ?? shortLabel;

  return {
    code,
    shortLabel,
    longLabel,
    providerCode,
    providerShortLabel: providerCode,
    providerLongLabel,
    providerLogoUrl: providerLogoUrl(providerCode),
    channelLogoUrl: channelLogoUrl(code),
    logoText: firstText(providerCode, shortLabel)?.slice(0, 4).toUpperCase() ?? '—',
  };
}

export function consoleResultSlotIdentity(input: ConsoleResultSlotIdentityInput): ConsoleResultSlotIdentity {
  const slotKey = clean(input.slotKey);
  const providerCode = providerCodeFrom(input.providerCode, slotKey);
  const slotShortLabel = firstText(input.slotLabel, slotLabelFromKey(slotKey), slotKey, providerCode, 'Slot') ?? 'Slot';
  const providerLongLabel = providerLabel(providerCode, input.providerName);

  return {
    slotKey,
    shortLabel: slotShortLabel,
    longLabel: firstText(input.slotLabel, joinLabels(providerLongLabel, slotShortLabel), slotShortLabel) ?? slotShortLabel,
    providerCode,
    providerShortLabel: providerCode,
    providerLongLabel,
    providerLogoUrl: providerLogoUrl(providerCode),
    slotLogoUrl: slotLogoUrl(slotKey) ?? providerLogoUrl(providerCode),
    logoText: firstText(providerCode, slotShortLabel)?.slice(0, 4).toUpperCase() ?? '—',
  };
}

/**
 * Heure locale (tenant) d'un tirage, formatée pour affichage — jamais l'heure provider.
 * Le tirage n'affiche sa date locale que si elle diffère de la date provider (même logique que
 * l'ancien `localLabel()` de `ConsoleDrawSlotIdentityComponent`, extraite ici pour être réutilisée
 * hors du composant d'identité — ex. sous le badge d'état d'une carte).
 */
export function consoleDrawSlotLocalLabel(identity: ConsoleDrawSlotIdentity): string | null {
  const showLocalDate = Boolean(
    identity.localDateLabel && identity.providerDateLabel && identity.localDateLabel !== identity.providerDateLabel,
  );
  const dateTime = [showLocalDate ? identity.localDateLabel : null, identity.localTimeLabel]
    .filter(Boolean)
    .join(' ');
  if (!dateTime) return null;
  return identity.localTimezoneLabel ? `${dateTime} · ${identity.localTimezoneLabel}` : dateTime;
}

export function consoleLotteryProviderLogoUrl(providerCode?: string | null): string | null {
  return providerLogoUrl(providerCode);
}

export function consoleLotterySlotLogoUrl(slotKey?: string | null): string | null {
  return slotLogoUrl(slotKey) ?? providerLogoUrl(providerCodeFrom(null, slotKey));
}

export function consoleLotteryChannelLogoUrl(channelCode?: string | null): string | null {
  return channelLogoUrl(channelCode);
}

export function consoleLotteryProviderCodeFromSlot(slotKey?: string | null): string | null {
  return providerCodeFrom(null, slotKey);
}

function providerCodeFrom(
  explicit?: string | null,
  slotKey?: string | null,
  channelCode?: string | null,
): string | null {
  const explicitCode = clean(explicit)?.split(/[_-]/, 1)[0]?.toUpperCase();
  if (explicitCode) return explicitCode;

  const fromSlot = providerCodeFromStructuredCode(slotKey);
  if (fromSlot) return fromSlot;

  const fromChannel = providerCodeFromStructuredCode(channelCode);
  return fromChannel || null;
}

function providerCodeFromStructuredCode(value?: string | null): string | null {
  const parts = clean(value)?.split(/[_-]/).filter(Boolean);
  if (!parts?.length) return null;
  const first = parts[0]?.toUpperCase();
  if (first === 'HT' && parts.length > 1) return parts[1]?.toUpperCase() ?? null;
  return first || null;
}

function providerLabel(providerCode?: string | null, explicit?: string | null): string | null {
  const explicitLabel = clean(explicit);
  if (explicitLabel && explicitLabel.toUpperCase() !== providerCode) return explicitLabel;
  return providerCode ? PROVIDER_LABELS[providerCode] ?? providerCode : null;
}

function slotLabelFromKey(slotKey?: string | null): string | null {
  const key = clean(slotKey);
  if (!key) return null;
  const parts = key.split(/[_-]/).filter(Boolean);
  const rawSlot = parts[parts.length - 1];
  if (!rawSlot) return null;
  return SLOT_LABELS[rawSlot.toUpperCase()] ?? titleCase(rawSlot);
}

function channelLabelFromCode(code?: string | null): string | null {
  const normalized = normalize(code);
  if (!normalized) return null;
  if (normalized.includes('numbers')) return 'Numbers';
  if (normalized.includes('win4')) return 'Win 4';
  if (normalized.includes('pick3') || normalized.includes('cash3') || normalized.includes('daily3')) return 'Pick 3';
  if (normalized.includes('pick4') || normalized.includes('cash4') || normalized.includes('daily4')) return 'Pick 4';
  return slotLabelFromKey(normalized);
}

function slotLogoUrl(slotKey?: string | null): string | null {
  const normalized = normalize(slotKey);
  if (!normalized) return null;
  const asset = SLOT_ASSETS[normalized];
  return asset ? `${TCH_LOTTERY_ASSET_BASE_PATH}/${asset}` : null;
}

function channelLogoUrl(channelCode?: string | null): string | null {
  const normalized = normalize(channelCode);
  if (!normalized) return null;
  const asset = CHANNEL_ASSETS[normalized] ?? inferChannelAsset(normalized);
  return asset ? `${TCH_LOTTERY_ASSET_BASE_PATH}/${asset}` : null;
}

function providerLogoUrl(providerCode?: string | null): string | null {
  const normalized = normalize(providerCode);
  if (!normalized) return null;
  const asset = PROVIDER_LOGO_ASSETS[normalized];
  return asset ? `${TCH_LOTTERY_LOGO_ASSET_BASE_PATH}/${asset}` : null;
}

function inferChannelAsset(value: string): string | null {
  if (value.includes('pick3') || value.includes('cash3') || value.includes('daily3')) return 'pick3-logo.png';
  if (value.includes('pick4') || value.includes('cash4') || value.includes('daily4')) return 'pick4-logo.png';
  if (value.includes('numbers')) return 'logo-numbers.png.webp';
  if (value.includes('win4')) return 'logo-win4.png.webp';
  return null;
}

function joinLabels(a?: string | null, b?: string | null): string | null {
  const left = clean(a);
  const right = clean(b);
  if (!left) return right;
  if (!right || right === left) return left;
  return `${left} · ${right}`;
}

function firstText(...values: readonly (string | null | undefined)[]): string | null {
  for (const value of values) {
    const text = clean(value);
    if (text) return text;
  }
  return null;
}

function clean(value?: string | null): string | null {
  const text = value?.trim();
  return text || null;
}

function normalize(value?: string | null): string | null {
  return clean(value)?.toLowerCase().replace(/_/g, '-') ?? null;
}

function titleCase(value: string): string {
  return value
    .toLowerCase()
    .split(/[\s_-]+/)
    .filter(Boolean)
    .map(part => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');
}
