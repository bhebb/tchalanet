import { TestBed } from '@angular/core/testing';
import { TranslateService, provideTranslateService } from '@ngx-translate/core';
import { describe, expect, it } from 'vitest';

import { DrawChannelProviderView } from '../../data-access/admin-draw-channels.models';
import { DrawChannelsSummaryComponent } from './draw-channels-summary.component';

describe(DrawChannelsSummaryComponent.name, () => {
  it('includes active, inactive and setup counts in the compact summary', () => {
    TestBed.configureTestingModule({
      imports: [DrawChannelsSummaryComponent],
      providers: [provideTranslateService()],
    });
    const translate = TestBed.inject(TranslateService);
    translate.setTranslation('ht', translations);
    translate.use('ht');

    const fixture = TestBed.createComponent(DrawChannelsSummaryComponent);
    fixture.componentRef.setInput('providers', providers);

    fixture.detectChanges();

    const text = normalizedText(fixture.nativeElement.textContent as string);
    expect(text).toMatch(/2\s*Founisè.*2\s*Aktif.*1\s*Inaktif.*1\s*Pou konfigire/);
  });
});

function normalizedText(value: string): string {
  return value.replace(/\s+/g, ' ').trim();
}

const providers: DrawChannelProviderView[] = [
  provider({
    providerCode: 'NY',
    tenantStatus: 'ACTIVE',
    slots: [
      { slotKey: 'NY_MID', label: 'NY · Midi', enabled: true },
      { slotKey: 'NY_EVE', label: 'NY · Soir', enabled: true },
    ],
  }),
  provider({
    providerCode: 'GA',
    tenantStatus: 'NEEDS_CONFIG',
    slots: [{ slotKey: 'GA_LATE', label: 'GA · Late', enabled: false }],
  }),
];

function provider(overrides: {
  providerCode: string;
  tenantStatus: DrawChannelProviderView['tenantStatus'];
  slots: DrawChannelProviderView['slots'];
}): DrawChannelProviderView {
  return {
    providerCode: overrides.providerCode,
    providerLabel: overrides.providerCode,
    timezone: 'America/New_York',
    tenantStatus: overrides.tenantStatus,
    resultAcquisition: {
      mode: 'AUTO',
      sourceStatus: 'OK',
      source: 'API',
    },
    slots: overrides.slots,
    readiness: {
      status: 'READY',
      label: 'ready',
      reason: null,
    },
  };
}

const translations = {
  admin: {
    drawChannels: {
      summary: {
        aria: 'Rezime konfigirasyon tiraj yo',
        availableStates: 'Founisè',
        activeChannels: 'Aktif',
        inactiveChannels: 'Inaktif',
        todo: 'Pou konfigire',
      },
    },
  },
};
