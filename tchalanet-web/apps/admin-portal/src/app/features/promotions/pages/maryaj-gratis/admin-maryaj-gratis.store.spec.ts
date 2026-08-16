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
    enableGame: ReturnType<typeof vi.fn>;
    disableGame: ReturnType<typeof vi.fn>;
  };
  let snackBar: { open: ReturnType<typeof vi.fn> };
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
      enableGame: vi.fn(),
      disableGame: vi.fn(),
    };
    snackBar = { open: vi.fn() };

    TestBed.configureTestingModule({
      providers: [
        AdminMaryajGratisStore,
        FormBuilder,
        { provide: AdminPromotionsApiService, useValue: promotionsApi },
        { provide: AdminGamesPricingApiService, useValue: gamesPricingApi },
        { provide: MatSnackBar, useValue: snackBar },
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

  it('adds a new open-ended tier after the previous maximum paid amount', () => {
    loadEditableCampaign();

    store.addQuantityTier();

    expect(store.quantityTiers().getRawValue()).toEqual([
      { minPaidAmount: 500, maxPaidAmount: 999, quantity: 2 },
      { minPaidAmount: 1000, maxPaidAmount: 1000, quantity: 4 },
      { minPaidAmount: 1001, maxPaidAmount: null, quantity: 1 },
    ]);
  });

  it('blocks saving a tiered offer when every quantity tier is removed', () => {
    loadEditableCampaign();
    store.quantityTiers().clear();

    store.saveOffer();

    expect(snackBar.open).toHaveBeenCalledWith(
      'admin.maryajGratis.validation.tiersRequired',
      'OK',
      { duration: 5000 },
    );
    expect(promotionsApi.updateCampaign).not.toHaveBeenCalled();
    expect(promotionsApi.updateRuleEffects).not.toHaveBeenCalled();
  });

  it('blocks saving a tier whose maximum paid amount is below its minimum', () => {
    loadEditableCampaign();
    store.quantityTiers().at(0).patchValue({
      minPaidAmount: 500,
      maxPaidAmount: 400,
    });

    store.saveOffer();

    expect(snackBar.open).toHaveBeenCalledWith(
      'admin.maryajGratis.validation.tierMaxBeforeMin',
      'OK',
      { duration: 5000 },
    );
    expect(promotionsApi.updateCampaign).not.toHaveBeenCalled();
    expect(promotionsApi.updateRuleEffects).not.toHaveBeenCalled();
  });

  it('blocks saving overlapping tier ranges', () => {
    loadEditableCampaign();
    store.quantityTiers().at(1).patchValue({
      minPaidAmount: 900,
      maxPaidAmount: null,
    });

    store.saveOffer();

    expect(snackBar.open).toHaveBeenCalledWith('admin.maryajGratis.validation.tierOverlap', 'OK', {
      duration: 5000,
    });
    expect(promotionsApi.updateCampaign).not.toHaveBeenCalled();
    expect(promotionsApi.updateRuleEffects).not.toHaveBeenCalled();
  });

  it('blocks saving when an open-ended tier is not last', () => {
    loadEditableCampaign();
    store.quantityTiers().at(0).patchValue({
      maxPaidAmount: null,
    });

    store.saveOffer();

    expect(snackBar.open).toHaveBeenCalledWith(
      'admin.maryajGratis.validation.openEndedLast',
      'OK',
      { duration: 5000 },
    );
    expect(promotionsApi.updateCampaign).not.toHaveBeenCalled();
    expect(promotionsApi.updateRuleEffects).not.toHaveBeenCalled();
  });

  it('treats a long-running persisted campaign as having no admin-facing end date', () => {
    const campaign = campaignWithTiers(undefined, {
      startsAt: '2026-07-01T00:00:00Z',
      endsAt: '2036-07-01T23:59:59Z',
    });
    promotionsApi.listCampaigns.mockReturnValue(of({ items: [campaign], total: 1 }));
    promotionsApi.getCampaign.mockReturnValue(of(campaign));
    gamesPricingApi.getGamesPricing.mockReturnValue(of([maryajGame()]));
    promotionsApi.updateCampaign.mockReturnValue(of(campaign));
    promotionsApi.updateRuleEffects.mockReturnValue(of(campaign));

    store.load();

    expect(store.form.controls.noEndDate.value).toBe(true);

    store.startEditingOffer();
    store.saveOffer();

    const request = promotionsApi.updateCampaign.mock.calls.at(-1)?.[1];
    expect(request).toEqual(
      expect.objectContaining({
        startsAt: expect.any(String),
        endsAt: expect.any(String),
      }),
    );
    expect(Date.parse(request.endsAt) - Date.parse(request.startsAt)).toBeGreaterThan(
      9 * 365 * 24 * 60 * 60 * 1000,
    );
  });

  it('keeps an explicit end date editable instead of converting it to no end date', () => {
    const campaign = campaignWithTiers(undefined, {
      startsAt: '2026-07-01T00:00:00Z',
      endsAt: '2026-12-31T23:59:59Z',
    });
    promotionsApi.listCampaigns.mockReturnValue(of({ items: [campaign], total: 1 }));
    promotionsApi.getCampaign.mockReturnValue(of(campaign));
    gamesPricingApi.getGamesPricing.mockReturnValue(of([maryajGame()]));
    promotionsApi.updateCampaign.mockReturnValue(of(campaign));
    promotionsApi.updateRuleEffects.mockReturnValue(of(campaign));

    store.load();

    expect(store.form.controls.noEndDate.value).toBe(false);

    store.startEditingOffer();
    store.form.controls.endsAt.setValue(new Date('2027-01-15T12:00:00Z'));
    store.saveOffer();

    const request = promotionsApi.updateCampaign.mock.calls.at(-1)?.[1];
    expect(Date.parse(request.endsAt) - Date.parse(request.startsAt)).toBeLessThan(
      365 * 24 * 60 * 60 * 1000,
    );
  });

  it('sends an edited start date and maps no-end offers to a long-running campaign', () => {
    const campaign = campaignWithTiers(undefined, {
      startsAt: '2026-07-01T00:00:00Z',
      endsAt: '2026-12-31T23:59:59Z',
    });
    promotionsApi.listCampaigns.mockReturnValue(of({ items: [campaign], total: 1 }));
    promotionsApi.getCampaign.mockReturnValue(of(campaign));
    gamesPricingApi.getGamesPricing.mockReturnValue(of([maryajGame()]));
    promotionsApi.updateCampaign.mockReturnValue(of(campaign));
    promotionsApi.updateRuleEffects.mockReturnValue(of(campaign));

    store.load();
    store.startEditingOffer();
    store.form.controls.startsAt.setValue(new Date(2026, 8, 15));
    store.form.controls.noEndDate.setValue(true);
    store.saveOffer();

    const request = promotionsApi.updateCampaign.mock.calls.at(-1)?.[1];
    expect(new Date(request.startsAt).getFullYear()).toBe(2026);
    expect(new Date(request.startsAt).getMonth()).toBe(8);
    expect(new Date(request.startsAt).getDate()).toBe(15);
    expect(Date.parse(request.endsAt) - Date.parse(request.startsAt)).toBeGreaterThan(
      9 * 365 * 24 * 60 * 60 * 1000,
    );
  });

  it('sends fixed attribution without tier or per-amount fields', () => {
    loadEditableCampaign();
    store.form.controls.quantityMode.setValue('FIXED');
    store.form.controls.quantity.setValue(3);

    store.saveOffer();

    const request = lastRuleEffectsRequest();
    expect(request.items[0].params).toEqual(
      expect.objectContaining({
        quantityMode: 'FIXED',
        quantity: 3,
      }),
    );
    expect(request.items[0].params['quantityTiers']).toBeUndefined();
    expect(request.items[0].params['stepPaidAmount']).toBeUndefined();
    expect(request.items[0].params['quantityPerStep']).toBeUndefined();
    expect(request.items[0].params['maxQuantity']).toBeUndefined();
  });

  it('sends per-amount attribution without tier fields', () => {
    loadEditableCampaign();
    store.form.controls.quantityMode.setValue('PER_PAID_AMOUNT');
    store.form.controls.stepPaidAmount.setValue(250);
    store.form.controls.quantityPerStep.setValue(2);
    store.form.controls.maxQuantity.setValue(8);

    store.saveOffer();

    const request = lastRuleEffectsRequest();
    expect(request.items[0].params).toEqual(
      expect.objectContaining({
        quantityMode: 'PER_PAID_AMOUNT',
        stepPaidAmount: 250,
        quantityPerStep: 2,
        maxQuantity: 8,
      }),
    );
    expect(request.items[0].params['quantityTiers']).toBeUndefined();
  });

  it('switches attribution mode controls between tiers, fixed, and per-amount fields', () => {
    loadEditableCampaign();
    const form = store.form;
    const tiers = store.quantityTiers();

    expect(form.controls.quantityMode.value).toBe('TIERED_PAID_AMOUNT');
    expect(tiers.enabled).toBe(true);
    expect(form.controls.stepPaidAmount.disabled).toBe(true);
    expect(form.controls.quantityPerStep.disabled).toBe(true);
    expect(form.controls.maxQuantity.disabled).toBe(true);

    form.controls.quantityMode.setValue('FIXED');

    expect(tiers.disabled).toBe(true);
    expect(form.controls.stepPaidAmount.disabled).toBe(true);
    expect(form.controls.quantityPerStep.disabled).toBe(true);
    expect(form.controls.maxQuantity.disabled).toBe(true);

    form.controls.quantityMode.setValue('PER_PAID_AMOUNT');

    expect(tiers.disabled).toBe(true);
    expect(form.controls.stepPaidAmount.enabled).toBe(true);
    expect(form.controls.quantityPerStep.enabled).toBe(true);
    expect(form.controls.maxQuantity.enabled).toBe(true);
    expect(form.controls.stepPaidAmount.value).toBe(1000);
    expect(form.controls.quantityPerStep.value).toBe(2);
    expect(form.controls.maxQuantity.value).toBe(10);

    form.controls.quantityMode.setValue('TIERED_PAID_AMOUNT');

    expect(tiers.enabled).toBe(true);
    expect(form.controls.stepPaidAmount.disabled).toBe(true);
    expect(form.controls.quantityPerStep.disabled).toBe(true);
    expect(form.controls.maxQuantity.disabled).toBe(true);
  });

  it('preserves advanced campaign priority when saving the offer', () => {
    loadEditableCampaign();
    store.form.controls.priority.setValue(77);

    store.saveOffer();

    expect(promotionsApi.updateCampaign).toHaveBeenCalledWith(
      'campaign-1',
      expect.objectContaining({
        priority: 77,
      }),
    );
  });

  it('pauses the campaign through the promotion API', () => {
    const activeCampaign = campaignWithTiers();
    const pausedCampaign: PromotionCampaignView = { ...activeCampaign, status: 'PAUSED' };
    loadCampaign(activeCampaign);
    promotionsApi.pauseCampaign.mockReturnValue(of(pausedCampaign));

    store.pause(activeCampaign);

    expect(promotionsApi.pauseCampaign).toHaveBeenCalledWith('campaign-1');
    expect(store.maryajCampaign()?.status).toBe('PAUSED');
    expect(store.saving()).toBe(false);
  });

  it('resumes the campaign through the promotion API', () => {
    const activeCampaign = campaignWithTiers();
    const pausedCampaign: PromotionCampaignView = { ...activeCampaign, status: 'PAUSED' };
    const resumedCampaign: PromotionCampaignView = { ...activeCampaign, status: 'ACTIVE' };
    loadCampaign(pausedCampaign);
    promotionsApi.activateCampaign.mockReturnValue(of(resumedCampaign));

    store.activate(pausedCampaign);

    expect(promotionsApi.activateCampaign).toHaveBeenCalledWith('campaign-1');
    expect(store.maryajCampaign()?.status).toBe('ACTIVE');
    expect(store.saving()).toBe(false);
  });

  it('enables and reloads the Maryaj Gratis game through the games API', () => {
    loadCampaign(campaignWithTiers());
    gamesPricingApi.enableGame.mockReturnValue(of(undefined));

    store.enableGame('HT_MARYAJ_GRATIS');

    expect(gamesPricingApi.enableGame).toHaveBeenCalledWith('HT_MARYAJ_GRATIS', {
      suppressShellFeedback: true,
    });
    expect(promotionsApi.listCampaigns).toHaveBeenCalledTimes(2);
    expect(store.saving()).toBe(false);
  });

  it('disables and reloads the Maryaj Gratis game through the games API', () => {
    loadCampaign(campaignWithTiers());
    gamesPricingApi.disableGame.mockReturnValue(of(undefined));

    store.disableGame('HT_MARYAJ_GRATIS');

    expect(gamesPricingApi.disableGame).toHaveBeenCalledWith('HT_MARYAJ_GRATIS', {
      suppressShellFeedback: true,
    });
    expect(promotionsApi.listCampaigns).toHaveBeenCalledTimes(2);
    expect(store.saving()).toBe(false);
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

  function loadEditableCampaign(): void {
    loadCampaign(campaignWithTiers());
    promotionsApi.updateCampaign.mockReturnValue(of(campaignWithTiers()));
    promotionsApi.updateRuleEffects.mockReturnValue(of(campaignWithTiers()));

    store.startEditingOffer();
  }

  function loadCampaign(campaign: PromotionCampaignView): void {
    promotionsApi.listCampaigns.mockReturnValue(of({ items: [campaign], total: 1 }));
    promotionsApi.getCampaign.mockReturnValue(of(campaign));
    gamesPricingApi.getGamesPricing.mockReturnValue(of([maryajGame()]));

    store.load();
  }

  function lastRuleEffectsRequest(): UpdatePromotionRuleEffectsRequest {
    const request = promotionsApi.updateRuleEffects.mock.calls.at(-1)?.[2];
    if (!request) throw new Error('Expected updateRuleEffects to be called');
    return request as UpdatePromotionRuleEffectsRequest;
  }
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
  metadata: Partial<Pick<PromotionCampaignView, 'startsAt' | 'endsAt'>> = {},
): PromotionCampaignView {
  return {
    id: 'campaign-1',
    code: 'DEFAULT_MARYAJ_GRATIS',
    name: 'Maryaj gratis',
    status: 'ACTIVE',
    priority: 10,
    startsAt: metadata.startsAt ?? null,
    endsAt: metadata.endsAt ?? null,
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
