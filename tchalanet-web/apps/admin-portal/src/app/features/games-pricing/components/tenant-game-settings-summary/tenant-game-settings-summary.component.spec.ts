import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { TranslateService, provideTranslateService } from '@ngx-translate/core';
import { describe, expect, it, vi } from 'vitest';

import { TenantGamePricingView } from '../../data-access/admin-games-pricing.models';
import { TenantGameSettingsSummaryComponent } from './tenant-game-settings-summary.component';

describe(TenantGameSettingsSummaryComponent.name, () => {
  it('renders row facts used by the games configuration list', () => {
    const fixture = createFixture(activeGame);

    fixture.detectChanges();

    const text = normalizedText(fixture.nativeElement.textContent as string);
    expect(text).toContain('Miz: 1-1000000 HTG');
    expect(text).toContain('Barèm: estanda · 1 opsyon');
    expect(text).toContain('Disponib pou vann');
    expect(text).toContain('Konfigire');
    expect(text.match(/Miz:/g)).toHaveLength(1);
    expect(text.match(/Barèm:/g)).toHaveLength(1);
  });

  it('renders the Maryaj summary presentation from the same stateless component', () => {
    const fixture = createFixture(maryajGame);
    fixture.componentRef.setInput('presentation', 'summary');
    fixture.componentRef.setInput('actionVariant', 'stroked');
    fixture.componentRef.setInput('actionLabelKey', 'admin.maryajGratis.game.configure');
    fixture.componentRef.setInput('availabilityRoute', '/app/admin/games/channel-matrix');

    fixture.detectChanges();

    const text = normalizedText(fixture.nativeElement.textContent as string);
    expect(text).toContain('Miz 1 HTG – 10 000 000 HTG');
    expect(text).toContain('Opsyon gany yoEgzak + ReverseBarèm estanda');
    expect(text).toContain('DisponibiliteJere sou chak tiraj Disponibilite pa tiraj →');
    expect(text).toContain('Modifye jwèt la');
  });

  it('emits the game when the configure action is selected', () => {
    const fixture = createFixture(activeGame);
    const configure = vi.fn();
    fixture.componentInstance.configure.subscribe(configure);

    fixture.detectChanges();
    fixture.nativeElement.querySelector('.tg-game-settings__primary-action').click();

    expect(configure).toHaveBeenCalledWith(activeGame);
  });

  it('uses business labels for known Maryaj payout variants', () => {
    const fixture = createFixture(maryajGame);
    const component = fixture.componentInstance;

    expect(component.gainOptionLabel('MARRIAGE_EXACT_ORDER', 'Exact order')).toBe('Egzak');
    expect(component.gainOptionLabel('MARRIAGE_REVERSE_ALLOWED', 'Reverse allowed')).toBe(
      'Reverse',
    );
    expect(component.gainOptionLabel('OTHER', 'Custom')).toBe('Custom');
  });
});

function createFixture(game: TenantGamePricingView) {
  TestBed.configureTestingModule({
    imports: [TenantGameSettingsSummaryComponent],
    providers: [provideNoopAnimations(), provideRouter([]), provideTranslateService()],
  });
  const translate = TestBed.inject(TranslateService);
  translate.setTranslation('ht', translations);
  translate.use('ht');

  const fixture = TestBed.createComponent(TenantGameSettingsSummaryComponent);
  fixture.componentRef.setInput('game', game);
  return fixture;
}

function normalizedText(value: string): string {
  return value.replace(/\s+/g, ' ').trim();
}

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

const maryajGame: TenantGamePricingView = {
  ...activeGame,
  gameCode: 'HT_MARYAJ_GRATIS',
  gameName: 'Maryaj gratis',
  pricingProfileLabel: 'admin.gamesPricing.card.standardPricingProfile',
  odds: [
    {
      label: 'Exact order',
      value: 'EXACT',
      odds: 500,
      payoutRuleType: 'STAKE_MULTIPLIER',
      fixedAmount: null,
      betType: 'MARRIAGE_2D2D',
      betOption: 1,
      pricingVariantCode: 'MARRIAGE_EXACT_ORDER',
    },
    {
      label: 'Reverse allowed',
      value: 'REVERSE',
      odds: 250,
      payoutRuleType: 'STAKE_MULTIPLIER',
      fixedAmount: null,
      betType: 'MARRIAGE_2D2D',
      betOption: 2,
      pricingVariantCode: 'MARRIAGE_REVERSE_ALLOWED',
    },
  ],
  limits: {
    minStake: 1,
    maxStake: 10_000_000,
    maxPerDraw: null,
    currency: 'HTG',
  },
};

const translations = {
  common: {
    not_available: 'Pa disponib',
  },
  admin: {
    gamesPricing: {
      card: {
        action: {
          configure: 'Konfigire',
        },
        availabilityReview: 'Disponib pou vann',
        availabilityNeedsConfig: 'Pou konfigire',
        availabilityUnavailable: 'Pa disponib',
        fact: {
          payouts: 'Barèm',
          stakes: 'Miz',
        },
        maryajPricing: 'Exact + Reverse',
        pricingMissing: 'Pa gen barèm',
        pricingOptions: '{{count}} opsyon',
        pricingOptionsWithProfile: '{{profile}} · {{count}} opsyon',
        stakeMissing: 'Pa gen miz',
        stakeRange: '{{min}}-{{max}} {{currency}}',
        stakeUnavailable: 'Pa disponib',
        standardPricingProfile: 'Barèm estanda',
      },
    },
    maryajGratis: {
      game: {
        availability: 'Disponibilite',
        availabilityAction: 'Disponibilite pa tiraj →',
        availabilitySummary: 'Jere sou chak tiraj',
        configure: 'Modifye jwèt la',
        exact: 'Egzak',
        notDefined: 'Pa defini',
        pricingOptions: 'Opsyon gany yo',
        reverse: 'Reverse',
        stake: 'Miz',
        standard: 'Barèm estanda',
      },
    },
  },
};
