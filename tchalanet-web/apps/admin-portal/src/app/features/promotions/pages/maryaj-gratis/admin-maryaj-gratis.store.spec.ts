import { TestBed } from '@angular/core/testing';
import { FormBuilder } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TranslateService } from '@ngx-translate/core';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import {
  AdminPromotionsApiService,
  PromotionCampaignView,
  UpdatePromotionRuleEffectsRequest,
} from '../../data-access/admin-promotions-api.service';
import { AdminGamesPricingApiService } from '../../../games-pricing/data-access/admin-games-pricing-api.service';
import { TenantGamePricingView } from '../../../games-pricing/data-access/admin-games-pricing.models';
import { AdminMaryajGratisStore } from './admin-maryaj-gratis.store';

describe(AdminMaryajGratisStore.name, () => {
  let promotionsApi: {
    listCampaigns: ReturnType<typeof vi.fn>;
    getCampaign: ReturnType<typeof vi.fn>;
    updateCampaign: ReturnType<typeof vi.fn>;
    updateRuleEffects: ReturnType<typeof vi.fn>;
    instantiateDefaultMaryajGratis: ReturnType<typeof vi.fn>;
    activateCampaign: ReturnType<typeof vi.fn>;
    pauseCampaign: ReturnType<typeof vi.fn>;
  };
  let gamesPricingApi: {
    getGamesPricing: ReturnType<typeof vi.fn>;
  };
  let store: AdminMaryajGratisStore;

  beforeEach(() => {
    promotionsApi = {
      listCampaigns: vi.fn(),
      getCampaign: vi.fn(),
      updateCampaign: vi.fn(),
      updateRuleEffects: vi.fn(),
      instantiateDefaultMaryajGratis: vi.fn(),
      activateCampaign: vi.fn(),
      pauseCampaign: vi.fn(),
    };
    gamesPricingApi = {
      getGamesPricing: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        AdminMaryajGratisStore,
        FormBuilder,
        { provide: AdminPromotionsApiService, useValue: promotionsApi },
        { provide: AdminGamesPricingApiService, useValue: gamesPricingApi },
        { provide: MatSnackBar, useValue: { open: vi.fn() } },
        { provide: TranslateService, useValue: { instant: (key: string) => key } },
      ],
    });

    store = TestBed.inject(AdminMaryajGratisStore);
  });

  it('hydrates quantity tiers from the persisted campaign instead of falling back to defaults', () => {
    promotionsApi.listCampaigns.mockReturnValue(of({ items: [campaignWithoutRules()], total: 1 }));
    promotionsApi.getCampaign.mockReturnValue(of(campaignWithTiers()));
    gamesPricingApi.getGamesPricing.mockReturnValue(of([maryajGame()]));

    store.load();

    expect(promotionsApi.getCampaign).toHaveBeenCalledWith('campaign-1');
    expect(store.state()).toBe('ready');
    expect(store.quantityTiers().getRawValue()).toEqual([
      { minPaidAmount: 500, maxPaidAmount: 999, quantity: 2 },
      { minPaidAmount: 1000, maxPaidAmount: null, quantity: 4 },
    ]);
  });

  it('sends the edited quantity tiers when saving, including tier deletion', () => {
    promotionsApi.listCampaigns.mockReturnValue(of({ items: [campaignWithTiers()], total: 1 }));
    promotionsApi.getCampaign.mockReturnValue(of(campaignWithTiers()));
    gamesPricingApi.getGamesPricing.mockReturnValue(of([maryajGame()]));
    promotionsApi.updateCampaign.mockReturnValue(of(campaignWithTiers()));
    promotionsApi.updateRuleEffects.mockReturnValue(
      of(campaignWithTiers([{ minPaidAmount: 700, maxPaidAmount: null, quantity: 5 }])),
    );

    store.load();
    store.startEditingOffer();
    store.removeQuantityTier(0);
    store.quantityTiers().at(0).patchValue({
      minPaidAmount: 700,
      maxPaidAmount: null,
      quantity: 5,
    });
    store.saveOffer();

    const calls = promotionsApi.updateRuleEffects.mock.calls;
    const request = calls[calls.length - 1][2] as UpdatePromotionRuleEffectsRequest;
    expect(promotionsApi.updateCampaign).toHaveBeenCalledWith(
      'campaign-1',
      expect.objectContaining({
        priority: 10,
      }),
    );
    expect(promotionsApi.updateRuleEffects).toHaveBeenCalledWith(
      'campaign-1',
      'rule-1',
      expect.any(Object),
    );
    expect(request.items[0].params['quantityTiers']).toEqual([
      { minPaidAmount: 700, maxPaidAmount: null, quantity: 5 },
    ]);
    expect(request.items[0].params['maxQuantity']).toBe(5);
  });

  it('keeps the page ready when tenant game configuration cannot be loaded', () => {
    promotionsApi.listCampaigns.mockReturnValue(of({ items: [], total: 0 }));
    gamesPricingApi.getGamesPricing.mockReturnValue(
      throwError(() => ({ status: 503, error: { title: 'Games unavailable' } })),
    );

    store.load();

    expect(store.state()).toBe('ready');
    expect(store.maryajGame()).toBeNull();
    expect(store.gamesError()).not.toBeNull();
  });
});

function campaignWithoutRules(): PromotionCampaignView {
  return {
    ...campaignWithTiers(),
    rules: [],
  };
}

function campaignWithTiers(
  tiers: readonly { minPaidAmount: number; maxPaidAmount: number | null; quantity: number }[] = [
    { minPaidAmount: 500, maxPaidAmount: 999, quantity: 2 },
    { minPaidAmount: 1000, maxPaidAmount: null, quantity: 4 },
  ],
): PromotionCampaignView {
  return {
    id: 'campaign-1',
    code: 'DEFAULT_MARYAJ_GRATIS',
    name: 'Maryaj gratis',
    status: 'ACTIVE',
    priority: 10,
    startsAt: null,
    endsAt: null,
    rules: [
      {
        id: 'rule-1',
        ruleKey: 'maryaj-gratis-default',
        priority: 10,
        eligibility: [],
        effects: [
          {
            type: 'FREE_GAME_LINE',
            params: {
              gameCode: 'HT_MARYAJ_GRATIS',
              payoutBaseAmount: 75,
              quantityMode: 'TIERED_PAID_AMOUNT',
              quantity: 1,
              maxQuantity: 4,
              quantityTiers: tiers,
              choiceMode: 'AUTO_GENERATE',
              generationStrategy: 'RANDOM',
              regenerableBeforeConfirm: true,
              maxRegenerationsBeforeConfirm: 3,
            },
          },
        ],
      },
    ],
  };
}

function maryajGame(): TenantGamePricingView {
  return {
    gameCode: 'HT_MARYAJ_GRATIS',
    tenantGameId: 'tenant-game-1',
    gameName: 'Maryaj gratis',
    catalogStatus: 'AVAILABLE',
    tenantStatus: 'ACTIVE',
    visibleInPos: true,
    pricingProfileLabel: null,
    odds: [],
    oddsGroups: [],
    limits: {
      minStake: 1,
      maxStake: 100,
      maxPerDraw: null,
      currency: 'HTG',
    },
    readiness: {
      status: 'READY',
      label: 'Prêt',
      reason: null,
    },
  };
}
