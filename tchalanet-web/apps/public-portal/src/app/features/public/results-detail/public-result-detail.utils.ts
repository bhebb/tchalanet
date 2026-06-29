import type { ResultStatus } from '@tch/page-model';

import type { PublicResultDetail, ResultStatusView } from './public-result-detail.model';

export function publicResultFallback(drawResultId: string | null): PublicResultDetail {
  const id = drawResultId?.trim() || 'fallback';
  return {
    drawResultId: id,
    slotKey: 'NY_MID',
    drawChannelLabelKey: 'draw_channel.ny.mid.label',
    drawChannelLabel: 'New York — Midi',
    resultDate: '2026-06-08',
    drawTime: '12:30',
    timezone: 'America/New_York',
    status: 'CONFIRMED',
    numbers: ['84', '12', '99', '24'],
    sourceLabel: 'New York Lottery',
    publishedAt: null,
    related: [
      { drawResultId: 'fl-eve-fallback', label: 'Florida Evening', status: 'PROVISIONAL' },
      { drawResultId: 'ga-mid-fallback', label: 'Georgia Midday', status: 'CONFIRMED' },
      { drawResultId: 'ny-eve-fallback', label: 'New York Evening', status: 'PROVISIONAL' },
    ],
  };
}

export function resultStatusView(status: ResultStatus): ResultStatusView {
  const views: Record<ResultStatus, ResultStatusView> = {
    PROVISIONAL: {
      icon: 'schedule',
      titleKey: 'public.results.detail_status.PROVISIONAL.title',
      bodyKey: 'public.results.detail_status.PROVISIONAL.body',
    },
    CONFIRMED: {
      icon: 'verified',
      titleKey: 'public.results.detail_status.CONFIRMED.title',
      bodyKey: 'public.results.detail_status.CONFIRMED.body',
    },
    OVERRIDDEN: {
      icon: 'edit_note',
      titleKey: 'public.results.detail_status.OVERRIDDEN.title',
      bodyKey: 'public.results.detail_status.OVERRIDDEN.body',
    },
    ERROR: {
      icon: 'error_outline',
      titleKey: 'public.results.detail_status.ERROR.title',
      bodyKey: 'public.results.detail_status.ERROR.body',
    },
  };
  return views[status];
}
