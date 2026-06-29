import { publicResultFallback, resultStatusView } from './public-result-detail.utils';

describe('PublicResultDetailPage helpers', () => {
  it('builds a fallback result from the drawResultId route param', () => {
    const result = publicResultFallback('9fe0a0b6-4cf2-42e6-9d3e-8c4d78d4beef');

    expect(result.drawResultId).toBe('9fe0a0b6-4cf2-42e6-9d3e-8c4d78d4beef');
    expect(result.status).toBe('CONFIRMED');
    expect(result.numbers).toEqual(['84', '12', '99', '24']);
    expect(result.slotKey).toBe('NY_MID');
    expect(result.drawChannelLabelKey).toBe('draw_channel.ny.mid.label');
  });

  it('uses a default fallback id when the param is null', () => {
    const result = publicResultFallback(null);
    expect(result.drawResultId).toBe('fallback');
  });

  it('maps result statuses to translated view copy keys', () => {
    expect(resultStatusView('CONFIRMED')).toEqual({
      icon: 'verified',
      titleKey: 'public.results.detail_status.CONFIRMED.title',
      bodyKey: 'public.results.detail_status.CONFIRMED.body',
    });
    expect(resultStatusView('PROVISIONAL')).toEqual({
      icon: 'schedule',
      titleKey: 'public.results.detail_status.PROVISIONAL.title',
      bodyKey: 'public.results.detail_status.PROVISIONAL.body',
    });
    expect(resultStatusView('OVERRIDDEN')).toEqual({
      icon: 'edit_note',
      titleKey: 'public.results.detail_status.OVERRIDDEN.title',
      bodyKey: 'public.results.detail_status.OVERRIDDEN.body',
    });
    expect(resultStatusView('ERROR')).toEqual({
      icon: 'error_outline',
      titleKey: 'public.results.detail_status.ERROR.title',
      bodyKey: 'public.results.detail_status.ERROR.body',
    });
  });
});
