const LOTTERY_ASSET_BASE = '/assets/images/lottery';

const SLOT_ASSETS: Record<string, string> = {
  'ca-daily3': 'ca-daily3.svg',
  'ca-daily4': 'ca-daily4.svg',
  'fl-middaycash3': 'fl-middaycash3.svg',
  'fl-play4midday': 'fl-play4midday.svg',
  'ga-midday3': 'ga-midday3.svg',
  'ga-midday4': 'ga-midday4.svg',
  'il-midday3': 'il-midday3.svg',
  'il-midday4': 'il-midday4.svg',
  'mi-daily3': 'mi-daily3.svg',
  'mi-midday4': 'mi-midday4.svg',
  'mn-daily3-new': 'mn-daily3-new.svg',
  'mo-middaypick3': 'mo-middaypick3.svg',
  'mo-middaypick4': 'mo-middaypick4.svg',
  'ms-cash3': 'ms-cash3.svg',
  'ms-cash4': 'ms-cash4.svg',
  'nj-middaypick4': 'nj-middaypick4.svg',
  'nj-pick3': 'nj-pick3.svg',
  'ny-middaynumbers': 'ny-middaynumbers.svg',
  'ny-middaywin4': 'ny-middaywin4.svg',
  'oh-middaypick4': 'oh-middaypick4.svg',
  'oh-pick3': 'oh-pick3.svg',
  'pa-middaypick4': 'pa-middaypick4.svg',
  'pa-pick3': 'pa-pick3.svg',
  'tn-cash3': 'tn-cash3.svg',
  'tn-morningcash4': 'tn-morningcash4.svg',
  'tx-middaypick3': 'tx-middaypick3.svg',
  'tx-morningpick4': 'tx-morningpick4.svg',
  'wi-dailypick4': 'wi-dailypick4.svg',
  'wi-pick3evening': 'wi-pick3evening.svg',
};

const DRAW_CHANNEL_ASSETS: Record<string, string> = {
  pick3: 'pick3-logo.png',
  pick4: 'pick4-logo.png',
  numbers: 'logo-numbers.png.webp',
  win4: 'logo-win4.png.webp',
};

export function lotteryAssetForSlot(slotKey?: string | null): string | null {
  const normalized = normalize(slotKey);
  if (!normalized) return null;
  const asset = SLOT_ASSETS[normalized];
  return asset ? `${LOTTERY_ASSET_BASE}/${asset}` : null;
}

export function lotteryAssetForDrawChannel(drawChannelCode?: string | null): string | null {
  const normalized = normalize(drawChannelCode);
  if (!normalized) return null;
  const asset = DRAW_CHANNEL_ASSETS[normalized] ?? inferChannelAsset(normalized);
  return asset ? `${LOTTERY_ASSET_BASE}/${asset}` : null;
}

export function lotteryAssetForProvider(providerCode?: string | null): string | null {
  return lotteryAssetForDrawChannel(providerCode);
}

function normalize(value?: string | null): string | null {
  const normalized = value?.trim().toLowerCase().replace(/_/g, '-');
  return normalized || null;
}

function inferChannelAsset(value: string): string | null {
  if (value.includes('pick3') || value.includes('cash3') || value.includes('daily3')) return 'pick3-logo.png';
  if (value.includes('pick4') || value.includes('cash4') || value.includes('daily4')) return 'pick4-logo.png';
  if (value.includes('numbers')) return 'logo-numbers.png.webp';
  if (value.includes('win4')) return 'logo-win4.png.webp';
  return null;
}
