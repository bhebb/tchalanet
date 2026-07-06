import { consoleTicketDrawIdentity } from './console-ticket-draw-identity';

describe('consoleTicketDrawIdentity', () => {
  it('prefers structured result slot identity over legacy receipt channel labels', () => {
    const view = consoleTicketDrawIdentity({
      channelCode: 'HT_GA_LATE',
      channelLabel: 'Haïti • Georgia • Late',
      resultSlotKey: 'GA_LATE',
      drawDateLabel: '2026-07-04',
      scheduledAt: '2026-07-05T02:30:00.000Z',
    });

    expect(view.identity.providerName).toBe('Georgia');
    expect(view.identity.channelName).toBe('Georgia · Late');
    expect(view.identity.providerLogoUrl).toContain('/assets/images/logo/ga_logo.png');
    expect(view.receiptLabel).toBe('Georgia · Late');
    expect(view.receiptDateTimeLabel).toContain('2026-07-04');
  });

  it('keeps channel label as a fallback when structured slot data is absent', () => {
    const view = consoleTicketDrawIdentity({
      channelCode: null,
      channelLabel: 'Canal spécial',
    });

    expect(view.receiptLabel).toBe('Canal spécial');
  });
});
