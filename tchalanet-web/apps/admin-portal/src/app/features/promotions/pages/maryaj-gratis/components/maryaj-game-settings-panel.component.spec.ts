import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { TranslateService, provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { TenantGamePricingView } from '../../../../games-pricing/data-access/admin-games-pricing.models';
import { MaryajGameSettingsPanelComponent } from './maryaj-game-settings-panel.component';

describe(MaryajGameSettingsPanelComponent.name, () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        provideTranslateService({
          fallbackLang: 'ht',
          lang: 'ht',
        }),
      ],
    });
    const translate = TestBed.inject(TranslateService);
    translate.setTranslation('ht', translations);
    translate.use('ht');
  });

  it('renders Maryaj Gratis with the shared tenant game card presentation', () => {
    const fixture = TestBed.createComponent(MaryajGameSettingsPanelComponent);
    fixture.componentRef.setInput('game', maryajGame());

    fixture.detectChanges();

    const text = normalizedText(fixture.nativeElement.textContent as string);
    expect(text).toContain('Maryaj gratis');
    expect(text).toContain('HT_MARYAJ_GRATIS');
    expect(text).toContain('Aktif');
    expect(text).toContain('An vant');
    expect(text).toContain('Miz: 1-10000000 HTG');
    expect(text).toContain('Barèm: Exact + Reverse');
    expect(text).toContain('Konfigire Maryaj gratis');
  });

  it('keeps incomplete Maryaj Gratis game state inside the shared card', () => {
    const fixture = TestBed.createComponent(MaryajGameSettingsPanelComponent);
    fixture.componentRef.setInput('game', maryajGame({ tenantStatus: 'NEEDS_CONFIG' }));

    fixture.detectChanges();

    const text = normalizedText(fixture.nativeElement.textContent as string);
    expect(text).toContain('Pou konfigire');
    expect(text).toContain('Konfigire Maryaj gratis');
  });

  it('forwards shared card configure and activation events to the page container', () => {
    const fixture = TestBed.createComponent(MaryajGameSettingsPanelComponent);
    const configure = vi.fn();
    const activate = vi.fn();
    const disable = vi.fn();
    fixture.componentRef.setInput('game', maryajGame());
    fixture.componentInstance.configure.subscribe(configure);
    fixture.componentInstance.activate.subscribe(activate);
    fixture.componentInstance.disable.subscribe(disable);

    fixture.componentInstance.onConfigure('HT_MARYAJ_GRATIS');
    fixture.componentInstance.activate.emit('HT_MARYAJ_GRATIS');
    fixture.componentInstance.disable.emit('HT_MARYAJ_GRATIS');

    expect(configure).toHaveBeenCalledWith(
      expect.objectContaining({ gameCode: 'HT_MARYAJ_GRATIS' }),
    );
    expect(activate).toHaveBeenCalledWith('HT_MARYAJ_GRATIS');
    expect(disable).toHaveBeenCalledWith('HT_MARYAJ_GRATIS');
  });

  it('renders a compact missing-game empty state', () => {
    const fixture = TestBed.createComponent(MaryajGameSettingsPanelComponent);
    fixture.componentRef.setInput('game', null);

    fixture.detectChanges();

    const text = normalizedText(fixture.nativeElement.textContent as string);
    expect(text).toContain('Pa konfigire');
    expect(text).toContain('Jwèt Maryaj gratis la poko disponib.');
  });
});

function normalizedText(value: string): string {
  return value.replace(/\s+/g, ' ').trim();
}

function maryajGame(overrides: Partial<TenantGamePricingView> = {}): TenantGamePricingView {
  return {
    gameCode: 'HT_MARYAJ_GRATIS',
    tenantGameId: 'tenant-game-1',
    gameName: 'Maryaj gratis',
    catalogStatus: 'AVAILABLE',
    tenantStatus: 'ACTIVE',
    visibleInPos: true,
    pricingProfileLabel: null,
    odds: [
      {
        label: 'Exact',
        value: '500',
        odds: 500,
        payoutRuleType: 'FIXED_AMOUNT',
        fixedAmount: 500,
        betType: 'MARRIAGE_2D2D',
        betOption: 1,
        pricingVariantCode: 'MARRIAGE_EXACT_ORDER',
      },
      {
        label: 'Reverse',
        value: '250',
        odds: 250,
        payoutRuleType: 'FIXED_AMOUNT',
        fixedAmount: 250,
        betType: 'MARRIAGE_2D2D',
        betOption: 2,
        pricingVariantCode: 'MARRIAGE_REVERSE_ALLOWED',
      },
    ],
    oddsGroups: [],
    limits: {
      minStake: 1,
      maxStake: 10_000_000,
      maxPerDraw: null,
      currency: 'HTG',
    },
    readiness: {
      status: 'TODO',
      label: 'Pou konfigire',
      reason: 'admin.gamesPricing.readiness.reason.missingStakeOrPricing',
    },
    ...overrides,
  };
}

const translations = {
  common: {
    not_available: 'Pa disponib',
  },
  admin: {
    gamesPricing: {
      card: {
        action: {
          maryaj: 'Konfigire Maryaj gratis',
        },
        availabilityNeedsConfig: 'Pou konfigire',
        availabilityReview: 'Disponib pou vann',
        availability: 'Disponibilite',
        availabilitySummary: 'Jere sou chak tiraj',
        configure: 'Konfigire',
        fact: {
          payouts: 'Barèm',
          stakes: 'Miz',
        },
        maryajPricing: 'Exact + Reverse',
        payoutOptions: 'Opsyon gany yo',
        pricingOptionsWithProfile: '{{profile}} · {{count}} opsyon',
        readiness: 'Disponib pou vann',
        saleDisabledShort: 'Pa vann',
        saleEnabledShort: 'An vant',
        stakeRange: '{{min}}-{{max}} {{currency}}',
        stakes: 'Miz',
        standardPricingProfile: 'estanda',
        toggle: {
          disable: 'Dezaktive',
          enable: 'Aktive',
        },
      },
      readiness: {
        TODO: 'Pou konfigire',
        reason: {
          missingStakeOrPricing: 'Miz oswa barèm manke.',
        },
      },
      status: {
        ACTIVE: 'Aktif',
        NEEDS_CONFIG: 'Pou konfigire',
        INACTIVE: 'Inaktif',
        UNAVAILABLE: 'Pa disponib',
      },
    },
    maryajGratis: {
      game: {
        configure: 'Modifye jwèt la',
        empty: {
          title: 'Jwèt Maryaj gratis la poko disponib.',
          copy: 'Konfigire jwèt la pou aktive òf gratis la.',
        },
        notConfigured: 'Pa konfigire',
        readiness: {
          READY: 'Pare',
          TODO: 'Pou konfigire',
          BLOCKED: 'Pa disponib',
        },
        subtitle: 'Menm lojik ak Maryaj.',
        title: 'Jwèt la',
      },
    },
  },
};
