import {
  consoleDrawChannelIdentity,
  consoleDrawIdentity,
  consoleLotteryProviderCodeFromSlot,
  consoleLotterySlotLogoUrl,
  consoleResultSlotIdentity,
} from './console-draw-identities';

describe('consoleResultSlotIdentity', () => {
  it('derives provider and slot labels from a slot key', () => {
    const identity = consoleResultSlotIdentity({ slotKey: 'ny-middaywin4' });

    expect(identity.providerCode).toBe('NY');
    expect(identity.providerLongLabel).toBe('New York');
    expect(identity.shortLabel).toBe('Middaywin4');
    expect(identity.longLabel).toBe('New York · Middaywin4');
    expect(identity.providerLogoUrl).toContain('/assets/images/logo/ny_logo.png');
    expect(identity.slotLogoUrl).toContain('/assets/images/lottery/ny_pick4.svg');
  });

  it('uses stable fallback labels for unknown slots without blanks', () => {
    const identity = consoleResultSlotIdentity({ slotKey: 'zz-night' });

    expect(identity.providerCode).toBe('ZZ');
    expect(identity.shortLabel).toBe('Night');
    expect(identity.longLabel).toBe('ZZ · Night');
    expect(identity.logoText).toBe('ZZ');
  });
});

describe('consoleDrawChannelIdentity', () => {
  it('derives provider label, product label, and logo from channel code', () => {
    const identity = consoleDrawChannelIdentity({ code: 'ny-middaynumbers' });

    expect(identity.providerCode).toBe('NY');
    expect(identity.providerLongLabel).toBe('New York');
    expect(identity.shortLabel).toBe('Numbers');
    expect(identity.longLabel).toBe('New York · Numbers');
    expect(identity.channelLogoUrl).toContain('/assets/images/lottery/logo-numbers.png.webp');
  });
});

describe('consoleDrawIdentity', () => {
  it('builds a structured console identity from draw/result fields', () => {
    const identity = consoleDrawIdentity({
      slotKey: 'tx-morningpick4',
      channelCode: 'tx-morningpick4',
      localDateLabel: '2026-07-04',
      localTimeLabel: '12:00',
      localTimezoneLabel: 'America/Toronto',
      officialTimeLabel: '11:00',
      officialTimezoneLabel: 'America/Chicago',
    });

    expect(identity.providerCode).toBe('TX');
    expect(identity.providerName).toBe('Texas');
    expect(identity.providerLogoUrl).toContain('/assets/images/logo/tx_logo.png');
    expect(identity.channelShortName).toBe('Pick 4');
    expect(identity.channelName).toBe('Texas · Pick 4');
    expect(identity.localDateLabel).toBe('2026-07-04');
    expect(identity.providerTimeLabel).toBe('11:00');
  });

  it('uses legacy channelName only as fallback when structured codes are available', () => {
    const identity = consoleDrawIdentity({
      slotKey: 'ny-middaynumbers',
      channelCode: 'ny-middaynumbers',
      channelName: 'Haïti · vieux nom seed',
    });

    expect(identity.channelShortName).toBe('Numbers');
    expect(identity.channelName).toBe('New York · Numbers');
  });

  it('turns matrix technical slot/channel codes into product labels', () => {
    const identity = consoleDrawIdentity({
      providerCode: 'GA',
      slotKey: 'HT_GA_LATE',
      channelCode: 'GA_LATE',
    });

    expect(identity.channelShortName).toBe('Late');
    expect(identity.channelName).toBe('Georgia · Late');
    expect(identity.slotLabel).toBe('Georgia · Late');
  });
});

describe('lottery asset helpers', () => {
  it('keeps compatibility helper behavior for provider and slot assets', () => {
    expect(consoleLotteryProviderCodeFromSlot('ny-middaynumbers')).toBe('NY');
    expect(consoleLotterySlotLogoUrl('ny-middaynumbers')).toContain('/assets/images/lottery/ny_pick3.svg');
  });
});
