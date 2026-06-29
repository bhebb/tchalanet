import { TCH_LOTTERY_ASSET_BASE_PATH } from '@tch/shared-assets';

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
  return asset ? `${TCH_LOTTERY_ASSET_BASE_PATH}/${asset}` : null;
}

export function lotteryAssetForDrawChannel(drawChannelCode?: string | null): string | null {
  const normalized = normalize(drawChannelCode);
  if (!normalized) return null;
  const asset = DRAW_CHANNEL_ASSETS[normalized] ?? inferChannelAsset(normalized);
  return asset ? `${TCH_LOTTERY_ASSET_BASE_PATH}/${asset}` : null;
}

export function lotteryAssetForProvider(providerCode?: string | null): string | null {
  return lotteryAssetForDrawChannel(providerCode);
}

function normalize(value?: string | null): string | null {
  const normalized = value?.trim().toLowerCase().replace(/_/g, '-');
  return normalized || null;
}

function inferChannelAsset(value: string): string | null {
  if (value.includes('pick3') || value.includes('cash3') || value.includes('daily3'))
    return 'pick3-logo.png';
  if (value.includes('pick4') || value.includes('cash4') || value.includes('daily4'))
    return 'pick4-logo.png';
  if (value.includes('numbers')) return 'logo-numbers.png.webp';
  if (value.includes('win4')) return 'logo-win4.png.webp';
  return null;
}
