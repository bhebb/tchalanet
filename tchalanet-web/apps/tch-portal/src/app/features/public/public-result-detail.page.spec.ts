import { publicResultFallback, resultStatusView } from './public-result-detail.page';

describe('PublicResultDetailPage helpers', () => {
  it('builds a PageModel-friendly fallback result from the route id', () => {
    const result = publicResultFallback('ny-afternoon');

    expect(result.id).toBe('ny-afternoon');
    expect(result.status).toBe('CONFIRMED');
    expect(result.numbers).toEqual(['84', '12', '99', '24']);
    expect(result.sourceLabelKey).toBe('public.results.fallback.source');
  });

  it('maps result statuses to translated view copy keys', () => {
    expect(resultStatusView('CONFIRMED')).toEqual({
      icon: 'verified',
      titleKey: 'public.results.detail_status.CONFIRMED.title',
      bodyKey: 'public.results.detail_status.CONFIRMED.body',
    });
    expect(resultStatusView('PENDING').icon).toBe('schedule');
    expect(resultStatusView('UNAVAILABLE').titleKey).toBe(
      'public.results.detail_status.UNAVAILABLE.title',
    );
  });
});
