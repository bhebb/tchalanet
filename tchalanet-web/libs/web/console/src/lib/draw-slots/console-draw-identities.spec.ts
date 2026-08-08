import {
  consoleDrawChannelIdentity,
  consoleDrawIdentity,
  consoleDrawSlotLocalLabel,
  consoleLotteryProviderCodeFromSlot,
  consoleLotterySlotLogoUrl,
  consoleResultSlotIdentity,
} from './console-draw-identities';
import { ConsoleDrawSlotIdentity } from './console-draw-slot-identity.models';

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

  it('uses the Wisconsin Lottery provider logo for Wisconsin draws', () => {
    const identity = consoleDrawIdentity({
      providerCode: 'WI',
      slotKey: 'WI_MID',
      channelCode: 'WI_MID',
    });

    expect(identity.providerCode).toBe('WI');
    expect(identity.providerName).toBe('Wisconsin');
    expect(identity.providerLogoUrl).toContain('/assets/images/logo/wi_logo.png');
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

  it('derives the provider from Haiti-prefixed platform channel codes', () => {
    const identity = consoleDrawIdentity({
      slotKey: 'HT_NY_MID',
      channelCode: 'HT_NY_MID',
      channelShortName: 'HT-Midi',
      officialTimeLabel: '14:30',
      officialTimezoneLabel: 'America/New_York',
    });

    expect(identity.providerCode).toBe('NY');
    expect(identity.providerName).toBe('New York');
    expect(identity.providerLogoUrl).toContain('/assets/images/logo/ny_logo.png');
    expect(identity.logoText).toBe('NY');
  });

  it('keeps the same sample channel identity across public, admin, and platform inputs', () => {
    const publicIdentity = consoleDrawIdentity({
      slotKey: 'NY_MID',
      channelCode: 'NY_MID',
      officialDateLabel: '2026-07-04',
      officialTimeLabel: '14:30',
      officialTimezoneLabel: 'America/New_York',
    });
    const adminIdentity = consoleDrawIdentity({
      slotKey: 'NY_MID',
      channelCode: 'NY_MID',
      channelName: 'Haïti · New York · Midday',
      officialDateLabel: '2026-07-04',
      officialTimeLabel: '14:30',
      officialTimezoneLabel: 'America/New_York',
      localDateLabel: '2026-07-04',
      localTimeLabel: '14:30',
      localTimezoneLabel: 'America/Toronto',
    });
    const platformIdentity = consoleDrawIdentity({
      providerCode: 'NY',
      slotKey: 'HT_NY_MID',
      channelCode: 'HT_NY_MID',
      channelName: 'Haïti · New York · Midday',
      officialTimeLabel: '14:30',
      officialTimezoneLabel: 'America/New_York',
    });

    for (const identity of [publicIdentity, adminIdentity, platformIdentity]) {
      expect(identity.providerCode).toBe('NY');
      expect(identity.providerName).toBe('New York');
      expect(identity.providerLogoUrl).toContain('/assets/images/logo/ny_logo.png');
      expect(identity.channelShortName).toBe('Midday');
      expect(identity.channelName).toBe('New York · Midday');
      expect(identity.channelName).not.toContain('Haïti');
    }

    expect(adminIdentity.localDateLabel).toBe('2026-07-04');
    expect(adminIdentity.localTimeLabel).toBe('14:30');
  });
});

describe('lottery asset helpers', () => {
  it('keeps compatibility helper behavior for provider and slot assets', () => {
    expect(consoleLotteryProviderCodeFromSlot('ny-middaynumbers')).toBe('NY');
    expect(consoleLotterySlotLogoUrl('ny-middaynumbers')).toContain('/assets/images/lottery/ny_pick3.svg');
  });
});

describe('consoleDrawSlotLocalLabel', () => {
  const baseIdentity: ConsoleDrawSlotIdentity = {
    localDateLabel: '2026-07-29',
    localTimeLabel: '19:00',
    localTimezoneLabel: 'America/Port-au-Prince',
    providerDateLabel: '2026-07-29',
    providerTimeLabel: '19:00',
    providerTimezoneLabel: 'America/New_York',
  };

  it('never includes the provider time or timezone', () => {
    const label = consoleDrawSlotLocalLabel(baseIdentity);
    expect(label).not.toContain('America/New_York');
    expect(label).toBe('19:00 · America/Port-au-Prince');
  });

  it('omits the local date when it matches the provider date', () => {
    const label = consoleDrawSlotLocalLabel(baseIdentity);
    expect(label).not.toContain('2026-07-29');
  });

  it('includes the local date only when it differs from the provider date', () => {
    const label = consoleDrawSlotLocalLabel({
      ...baseIdentity,
      localDateLabel: '2026-07-30',
    });
    expect(label).toBe('2026-07-30 19:00 · America/Port-au-Prince');
  });

  it('returns null when there is no local time to show', () => {
    expect(consoleDrawSlotLocalLabel({})).toBeNull();
  });
});
