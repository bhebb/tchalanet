import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { TestBed } from '@angular/core/testing';
import { TranslateService, provideTranslateService } from '@ngx-translate/core';
import { describe, expect, it } from 'vitest';

import { TenantGamePricingView } from '../../data-access/admin-games-pricing.models';
import { TenantGameCardComponent } from './tenant-game-card.component';

describe(TenantGameCardComponent.name, () => {
  it('disables game mutation actions when the user cannot manage game pricing', () => {
    TestBed.configureTestingModule({
      imports: [TenantGameCardComponent],
      providers: [provideNoopAnimations(), provideTranslateService()],
    });
    const translate = TestBed.inject(TranslateService);
    translate.setTranslation('ht', translations);
    translate.use('ht');

    const fixture = TestBed.createComponent(TenantGameCardComponent);
    fixture.componentRef.setInput('game', activeGame);
    fixture.componentRef.setInput('canManage', false);

    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.gp-game-card__sale-toggle').disabled).toBe(true);
    expect(fixture.nativeElement.querySelector('.gp-game-card__primary-action').disabled).toBe(true);
  });

  it('uses the pending sale state while the game mutation is saving', () => {
    TestBed.configureTestingModule({
      imports: [TenantGameCardComponent],
      providers: [provideNoopAnimations(), provideTranslateService()],
    });
    const translate = TestBed.inject(TranslateService);
    translate.setTranslation('ht', translations);
    translate.use('ht');

    const fixture = TestBed.createComponent(TenantGameCardComponent);
    fixture.componentRef.setInput('game', activeGame);
    fixture.componentRef.setInput('pendingEnabled', false);

    fixture.detectChanges();

    const toggle = fixture.nativeElement.querySelector('.gp-game-card__sale-toggle');
    expect(toggle.classList.contains('gp-game-card__sale-toggle--on')).toBe(false);
    expect(toggle.getAttribute('aria-pressed')).toBe('false');
  });
});

const activeGame: TenantGamePricingView = {
  gameCode: 'HT_BOLET',
  tenantGameId: 'tenant-game-bolet',
  gameName: 'Bolèt',
  catalogStatus: 'AVAILABLE',
  tenantStatus: 'ACTIVE',
  visibleInPos: true,
  pricingProfileLabel: 'Barèm estanda',
  odds: [
    {
      label: 'Chif',
      value: '12',
      odds: 50,
      payoutRuleType: 'STAKE_MULTIPLIER',
      fixedAmount: null,
      betType: 'BOLET',
      betOption: null,
      pricingVariantCode: 'BOLET_DEFAULT',
    },
  ],
  oddsGroups: [],
  limits: {
    minStake: 1,
    maxStake: 1_000_000,
    maxPerDraw: null,
    currency: 'HTG',
  },
  readiness: {
    status: 'READY',
    label: 'Ready',
    reason: null,
  },
};

const translations = {
  admin: {
    gamesPricing: {
      card: {
        action: {
          complete: 'Konplete',
          configure: 'Konfigire',
          maryaj: 'Konfigire Maryaj gratis',
        },
        availabilityReview: 'Disponib pou vann',
        availabilityNeedsConfig: 'Pou konfigire',
        availabilityUnavailable: 'Pa disponib',
        fact: {
          payouts: 'Barèm',
          stakes: 'Miz',
        },
        pricingMissing: 'Pa gen barèm',
        pricingOptions: '{{count}} opsyon',
        pricingOptionsWithProfile: '{{profile}} · {{count}} opsyon',
        saleDisabledShort: 'Fèmen',
        saleEnabledShort: 'An vant',
        stakeMissing: 'Pa gen miz',
        stakeRange: '{{min}}-{{max}} {{currency}}',
        stakeUnavailable: 'Pa disponib',
        toggle: {
          disable: 'Dezaktive',
          enable: 'Aktive',
        },
      },
      readiness: {
        TODO: 'Pou konfigire',
      },
      status: {
        ACTIVE: 'Aktif',
        INACTIVE: 'Inaktif',
        NEEDS_CONFIG: 'Pou konfigire',
        UNAVAILABLE: 'Pa disponib',
      },
    },
  },
};
