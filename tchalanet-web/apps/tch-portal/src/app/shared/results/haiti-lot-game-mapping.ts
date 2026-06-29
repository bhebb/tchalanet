import { TCH_LOTTERY_ASSET_BASE_PATH } from '@tch/shared-assets';

export type HaitiLotKey = 'lot1' | 'lot2' | 'lot3';
export type ProviderGameKind = 'pick3' | 'pick4';

export interface HaitiLotGameMappingSource {
  readonly slotKey?: unknown;
  readonly provider?: unknown;
}

export interface HaitiLotGameMapping {
  readonly lotKey: HaitiLotKey;
  readonly label: string;
  readonly gameKind: ProviderGameKind;
  readonly gameLabel: string;
  readonly provider: string;
  readonly imageSrc: string;
  readonly imageAlt: string;
}

const LOTTERY_IMAGE_BASE = `${TCH_LOTTERY_ASSET_BASE_PATH}/`;

const PROVIDER_GAME_LABELS: Record<string, { pick3Label?: string; pick4Label?: string }> = {
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

const FALLBACK_GAME_IMAGES = {
  pick3: 'pick3-logo.png',
  pick4: 'pick4-logo.png',
};

const PROVIDER_GAME_IMAGE_OVERRIDES: Partial<Record<`${string}:${ProviderGameKind}`, string>> = {
  'MN:pick4': FALLBACK_GAME_IMAGES.pick4,
  'TX:pick4': 'tx_pick4.png',
};

export function haitiLotGameMappings(
  source: HaitiLotGameMappingSource | null | undefined,
): HaitiLotGameMapping[] {
  const provider = resolveProvider(source);
  const labels = PROVIDER_GAME_LABELS[provider] ?? {};

  return [
    toMapping('lot1', '1er lot', 'pick3', provider, labels),
    toMapping('lot2', '2e lot', 'pick4', provider, labels),
    toMapping('lot3', '3e lot', 'pick4', provider, labels),
  ];
}

function toMapping(
  lotKey: HaitiLotKey,
  label: string,
  gameKind: ProviderGameKind,
  provider: string,
  labels: { pick3Label?: string; pick4Label?: string },
): HaitiLotGameMapping {
  const gameLabel =
    gameKind === 'pick3' ? (labels.pick3Label ?? 'Pick 3') : (labels.pick4Label ?? 'Pick 4');

  return {
    lotKey,
    label,
    gameKind,
    gameLabel,
    provider,
    imageSrc: LOTTERY_IMAGE_BASE + providerGameImage(provider, gameKind),
    imageAlt: `${provider} ${gameLabel}`,
  };
}

function providerGameImage(provider: string, gameKind: ProviderGameKind): string {
  const override = PROVIDER_GAME_IMAGE_OVERRIDES[`${provider}:${gameKind}`];
  if (override) return override;
  if (provider === 'US') return FALLBACK_GAME_IMAGES[gameKind];
  return `${provider.toLowerCase()}_${gameKind}.svg`;
}

function resolveProvider(source: HaitiLotGameMappingSource | null | undefined): string {
  const explicit = clean(source?.provider);
  if (explicit) return explicit.toUpperCase();

  const slotKey = clean(source?.slotKey);
  const inferred = slotKey?.split(/[_-]/)[0];
  return inferred?.toUpperCase() || 'US';
}

function clean(value: unknown): string | null {
  const trimmed = typeof value === 'string' ? value.trim() : '';
  return trimmed || null;
}
