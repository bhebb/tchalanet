import {
  consoleLotteryChannelLogoUrl,
  consoleLotteryProviderCodeFromSlot,
  consoleLotterySlotLogoUrl,
  consoleResultSlotIdentity,
} from '../draw-slots/console-draw-identities';

export type ConsoleHaitiLotKey = 'lot1' | 'lot2' | 'lot3';
export type ConsoleProviderGameKind = 'pick3' | 'pick4';

export interface ConsoleHaitiLotGameMappingSource {
  readonly slotKey?: unknown;
  readonly provider?: unknown;
  readonly providerCode?: unknown;
  readonly providerName?: unknown;
}

export interface ConsoleHaitiLotGameMapping {
  readonly lotKey: ConsoleHaitiLotKey;
  readonly haitiLotLabel: string;
  readonly providerCode: string;
  readonly providerShortLabel: string;
  readonly providerLongLabel: string;
  readonly providerGameKind: ConsoleProviderGameKind;
  readonly providerGameLabel: string;
  readonly providerGameLogoUrl: string | null;
  readonly providerGameLogoAlt: string;
}

const PROVIDER_GAME_LABELS: Record<string, { readonly pick3Label?: string; readonly pick4Label?: string }> = {
  CA: { pick3Label: 'Daily 3', pick4Label: 'Daily 4' },
  FL: { pick3Label: 'Cash 3', pick4Label: 'Play 4' },
  GA: { pick3Label: 'Midday 3', pick4Label: 'Midday 4' },
  IL: { pick3Label: 'Midday 3', pick4Label: 'Midday 4' },
  MI: { pick3Label: 'Daily 3', pick4Label: 'Daily 4' },
  MN: { pick3Label: 'Daily 3', pick4Label: 'Pick 4' },
  MS: { pick3Label: 'Cash 3', pick4Label: 'Cash 4' },
  NY: { pick3Label: 'Numbers', pick4Label: 'Win 4' },
  TN: { pick3Label: 'Cash 3', pick4Label: 'Cash 4' },
  TX: { pick3Label: 'Pick 3', pick4Label: 'Daily 4' },
  WI: { pick3Label: 'Pick 3', pick4Label: 'Daily Pick 4' },
};

export function consoleHaitiLotGameMappings(
  source: ConsoleHaitiLotGameMappingSource | null | undefined,
): readonly ConsoleHaitiLotGameMapping[] {
  const slotKey = clean(source?.slotKey);
  const providerCode = resolveProviderCode(source, slotKey);
  const providerName = clean(source?.providerName);
  const slotIdentity = consoleResultSlotIdentity({
    slotKey,
    providerCode,
    providerName,
  });
  const labels = PROVIDER_GAME_LABELS[providerCode] ?? {};

  return [
    toMapping('lot1', '1er lot', 'pick3', providerCode, slotIdentity.providerLongLabel, labels),
    toMapping('lot2', '2e lot', 'pick4', providerCode, slotIdentity.providerLongLabel, labels),
    toMapping('lot3', '3e lot', 'pick4', providerCode, slotIdentity.providerLongLabel, labels),
  ];
}

function toMapping(
  lotKey: ConsoleHaitiLotKey,
  haitiLotLabel: string,
  providerGameKind: ConsoleProviderGameKind,
  providerCode: string,
  providerLongLabel: string | null,
  labels: { readonly pick3Label?: string; readonly pick4Label?: string },
): ConsoleHaitiLotGameMapping {
  const providerGameLabel =
    providerGameKind === 'pick3' ? (labels.pick3Label ?? 'Pick 3') : (labels.pick4Label ?? 'Pick 4');
  const logoUrl = providerGameLogoUrl(providerCode, providerGameKind, providerGameLabel);

  return {
    lotKey,
    haitiLotLabel,
    providerCode,
    providerShortLabel: providerCode,
    providerLongLabel: providerLongLabel ?? providerCode,
    providerGameKind,
    providerGameLabel,
    providerGameLogoUrl: logoUrl,
    providerGameLogoAlt: `${providerCode} ${providerGameLabel}`,
  };
}

function providerGameLogoUrl(
  providerCode: string,
  providerGameKind: ConsoleProviderGameKind,
  _providerGameLabel: string,
): string | null {
  const slotLogo = consoleLotterySlotLogoUrl(`${providerCode}-${providerGameKind}`);
  if (slotLogo) return slotLogo;
  return consoleLotteryChannelLogoUrl(providerGameKind);
}

function resolveProviderCode(source: ConsoleHaitiLotGameMappingSource | null | undefined, slotKey: string | null): string {
  const explicit = clean(source?.providerCode) ?? clean(source?.provider);
  const providerCode = explicit?.split(/[_-]/, 1)[0]?.toUpperCase() ?? consoleLotteryProviderCodeFromSlot(slotKey);
  return providerCode || 'US';
}

function clean(value: unknown): string | null {
  const trimmed = typeof value === 'string' ? value.trim() : '';
  return trimmed || null;
}
