import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { form } from '@angular/forms/signals';
import { provideRouter } from '@angular/router';
import { TranslateService, provideTranslateService } from '@ngx-translate/core';
import { describe, expect, it, vi } from 'vitest';

import { TenantGameOddGroupView } from '../../data-access/admin-games-pricing.models';
import { TenantGameBetOptionConfigView, TenantGameView } from '../../data-access/games-admin-api.service';
import {
  GameSettingsEditorComponent,
  GameSettingsFormModel,
} from './game-settings-editor.component';

describe(GameSettingsEditorComponent.name, () => {
  it('renders the shared game editor sections without owning save state', () => {
    const fixture = createFixture();
    fixture.componentRef.setInput('betOptionConfig', betOptionConfig);
    fixture.componentRef.setInput('pricingGroups', pricingGroups);
    fixture.componentRef.setInput('showSalesOptions', true);

    fixture.detectChanges();

    const text = normalizedText(fixture.nativeElement.textContent as string);
    expect(text).toContain('Vant');
    expect(text).toContain('Miz');
    expect(text).toContain('Disponibilite');
    expect(text).toContain('Opsyon jwèt');
    expect(text).toContain('Barèm');
    expect(text).toContain('Opsyon avanse');
  });

  it('emits edit intentions for options and payout rules', () => {
    const fixture = createFixture();
    fixture.componentRef.setInput('betOptionConfig', betOptionConfig);
    fixture.componentRef.setInput('pricingGroups', pricingGroups);
    fixture.componentRef.setInput('showSalesOptions', true);
    fixture.componentRef.setInput('editingPricingVariantKey', 'maryaj:MARYAJ_EXACT:Egzak');
    const pricingOdds = vi.fn();
    fixture.componentInstance.pricingOddsChange.subscribe(pricingOdds);

    fixture.detectChanges();
    const oddsInput = fixture.nativeElement.querySelector('input[min="0.0001"]') as HTMLInputElement;
    oddsInput.value = '4.5';
    oddsInput.dispatchEvent(new Event('input'));

    expect(pricingOdds).toHaveBeenCalledWith({
      groupId: 'maryaj',
      pricingVariantCode: 'MARYAJ_EXACT',
      rawValue: '4.5',
    });
  });
});

function createFixture() {
  TestBed.configureTestingModule({
    imports: [GameSettingsEditorComponent],
    providers: [provideNoopAnimations(), provideRouter([]), provideTranslateService()],
  });
  const translate = TestBed.inject(TranslateService);
  translate.setTranslation('ht', translations);
  translate.use('ht');

  const model = signal<GameSettingsFormModel>({
    displayName: 'Maryaj gratis',
    visibleInPos: true,
    minStake: 1,
    maxStake: 10_000_000,
    displayOrder: 0,
    availabilityEnabled: false,
    startLocalTime: '',
    endLocalTime: '',
  });
  const fixture = TestBed.createComponent(GameSettingsEditorComponent);
  fixture.componentRef.setInput('game', game);
  fixture.componentRef.setInput('form', TestBed.runInInjectionContext(() => form(model)));
  fixture.componentRef.setInput('model', model());
  fixture.componentRef.setInput('selectionPolicies', [
    'EXPLICIT_ONLY',
    'EXPLICIT_WITH_AUTO_OPTION',
    'IMPLICIT_BEST_MATCH',
  ]);
  return fixture;
}

function normalizedText(value: string): string {
  return value.replace(/\s+/g, ' ').trim();
}

const game: TenantGameView = {
  gameCode: 'HT_MARYAJ_GRATIS',
  catalogName: 'Maryaj gratis',
  category: null,
  displayName: 'Maryaj gratis',
  enabled: true,
  visibleInPos: true,
  displayOrder: 0,
  minStake: 1,
  maxStake: 10_000_000,
  availabilityEnabled: false,
  availabilityDays: null,
  startLocalTime: null,
  endLocalTime: null,
  readyForSale: true,
  betOptions: [],
  betOptionGroups: [],
};

const betOptionConfig: TenantGameBetOptionConfigView = {
  gameCode: 'HT_MARYAJ_GRATIS',
  betTypes: [
    {
      betType: 'MARRIAGE_2D2D',
      selectionPolicy: 'EXPLICIT_ONLY',
      defaultOption: null,
      options: [
        {
          code: 1,
          label: 'Egzak',
          description: 'Egzak',
          enabled: true,
          visibleInPos: true,
          displayOrder: 1,
        },
      ],
    },
  ],
};

const pricingGroups: readonly TenantGameOddGroupView[] = [
  {
    id: 'maryaj',
    label: 'Maryaj',
    betType: 'MARRIAGE_2D2D',
    betOption: null,
    variants: [
      {
        label: 'Egzak',
        value: '500',
        odds: 500,
        payoutRuleType: 'STAKE_MULTIPLIER',
        fixedAmount: null,
        betType: 'MARRIAGE_2D2D',
        betOption: 1,
        pricingVariantCode: 'MARYAJ_EXACT',
      },
    ],
  },
];

const translations = {
  common: {
    loading: 'Y ap chaje',
    not_available: 'Pa disponib',
  },
  admin: {
    gamesPricing: {
      card: {
        notConfigured: 'Pa konfigire',
      },
    },
    games: {
      settings: {
        intro: {
          aria: 'Entwodiksyon',
          title: 'Paramèt jwèt la',
          message: 'Konfigire jwèt la.',
        },
        sell: {
          title: 'Vant',
          description: 'Kontwole vant.',
          activationTitle: 'Jwèt aktive',
          activationHelp: 'Aktivasyon fèt nan lis jwèt yo.',
          activationEnabled: 'Aktive',
          activationDisabled: 'Inaktif',
          visibleInPosHelp: 'Kontwole POS.',
        },
        stakes: {
          title: 'Miz',
          description: 'Limit miz yo.',
        },
        availability: {
          title: 'Disponibilite',
          description: 'Règ tiraj.',
          enabledHelp: 'Swiv règ tiraj yo.',
          review: 'Disponibilite pa tiraj',
        },
        salesOptions: {
          title: 'Opsyon jwèt',
          description: 'Opsyon vann yo.',
          optionControlsHelp: 'Kontwòl opsyon.',
          selectionPolicy: 'Seleksyon',
          selectionPolicyHelp: 'Kontwole seleksyon.',
          defaultOption: 'Opsyon default',
          noDefaultOption: 'Pa gen',
          enabled: 'Ofri',
          visibleInPos: 'Vizib POS',
          policy: {
            EXPLICIT_ONLY: 'Manyèl',
            EXPLICIT_WITH_AUTO_OPTION: 'Manyèl + auto',
            IMPLICIT_BEST_MATCH: 'Auto',
          },
        },
        payouts: {
          title: 'Barèm',
          description: 'Règ gany yo.',
          editPricing: 'Modifye',
          notConfigured: 'Pa konfigire',
          multiplierSummary: 'x{{odds}}',
          fixedAmountSummary: '{{amount}}',
        },
        advanced: {
          title: 'Opsyon avanse',
          summary: 'Non POS ak lòd',
        },
        field: {
          displayName: 'Non',
          displayOrder: 'Lòd',
          minStake: 'Minimum',
          maxStake: 'Maximum',
          visibleInPos: 'Vizib POS',
          availabilityEnabled: 'Swiv règ tiraj',
          startLocalTime: 'Kòmansman',
          endLocalTime: 'Fen',
        },
        options: {
          clearPricing: 'Efase',
        },
      },
    },
  },
  console: {
    pricingForm: {
      field: {
        payoutRuleType: 'Kalite',
        fixedAmount: 'Montan',
      },
      payoutRuleType: {
        STAKE_MULTIPLIER: 'Miltiplikatè',
        FIXED_AMOUNT: 'Montan fiks',
      },
    },
  },
  domain: {
    entity: {
      odds: 'Odds',
    },
  },
};
