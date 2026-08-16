import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';

import { AdminGamesPricingApiService } from '../../data-access/admin-games-pricing-api.service';
import {
  TenantGameOddGroupView,
  TenantGamePricingView,
} from '../../data-access/admin-games-pricing.models';
import {
  GamesAdminApiService,
  TenantGameBetOptionConfigView,
  TenantGameView,
} from '../../data-access/games-admin-api.service';
import { AdminGameSettingsPage } from './admin-game-settings.page';

describe(AdminGameSettingsPage.name, () => {
  it('loads the requested game and returns to Maryaj Gratis when opened from that flow', () => {
    const { page, router } = createPage({
      queryParams: { returnTo: 'maryaj-gratis', from: 'setup' },
    });

    expect(page.game()?.gameCode).toBe('HT_MARYAJ_GRATIS');
    expect(page.pageTitle()).toBe('Maryaj gratis');

    page.requestCancel();

    expect(router.navigate).toHaveBeenCalledWith(['/app/admin/maryaj-gratis'], {
      queryParams: { from: 'setup' },
      fragment: 'game',
    });
  });

  it('maps edited game, option, and payout settings through the page-owned APIs', () => {
    const { page, gamesApi, pricingApi, router } = createPage();
    page.model.set({
      displayName: 'Maryaj gratis POS',
      visibleInPos: false,
      minStake: 5,
      maxStake: 5000,
      displayOrder: 7,
      availabilityEnabled: true,
      startLocalTime: '08:00',
      endLocalTime: '20:00',
    });
    page.toggleOptionEnabled({
      betType: 'MARRIAGE_2D2D',
      optionCode: 2,
      checked: false,
    });
    page.updatePricingRuleType({
      groupId: 'maryaj',
      pricingVariantCode: 'MARYAJ_EXACT',
      payoutRuleType: 'FIXED_AMOUNT',
    });
    page.updatePricingFixedAmount({
      groupId: 'maryaj',
      pricingVariantCode: 'MARYAJ_EXACT',
      rawValue: '250',
    });
    page.requestClearPricingOdds({
      groupId: 'maryaj',
      pricingVariantCode: 'MARYAJ_REVERSE',
      label: 'Reverse',
    });

    page.submit(new Event('submit'));

    expect(gamesApi.updateGameSettings).toHaveBeenCalledWith(
      'HT_MARYAJ_GRATIS',
      {
        displayName: 'Maryaj gratis POS',
        visibleInPos: false,
        minStake: 5,
        maxStake: 5000,
        displayOrder: 7,
        availabilityEnabled: true,
        startLocalTime: '08:00',
        endLocalTime: '20:00',
      },
      { suppressShellFeedback: true },
    );
    expect(gamesApi.updateBetOptionConfig).toHaveBeenCalledWith(
      'HT_MARYAJ_GRATIS',
      expect.objectContaining({
        betTypes: [
          expect.objectContaining({
            betType: 'MARRIAGE_2D2D',
            options: expect.arrayContaining([
              expect.objectContaining({ code: 2, enabled: false, visibleInPos: false }),
            ]),
          }),
        ],
      }),
      { suppressShellFeedback: true },
    );
    expect(pricingApi.upsertTenantOdds).toHaveBeenCalledWith(
      expect.objectContaining({
        gameCode: 'HT_MARYAJ_GRATIS',
        pricingVariantCode: 'MARYAJ_EXACT',
        payoutRuleType: 'FIXED_AMOUNT',
        fixedAmount: 250,
        odds: null,
      }),
      { suppressShellFeedback: true },
    );
    expect(pricingApi.deleteTenantOdds).toHaveBeenCalledWith(
      {
        gameCode: 'HT_MARYAJ_GRATIS',
        pricingVariantCode: 'MARYAJ_REVERSE',
      },
      { suppressShellFeedback: true },
    );
    expect(router.navigate).toHaveBeenCalledWith(['/app/admin/games'], {
      queryParams: undefined,
      fragment: undefined,
    });
  });

  it('hydrates the editor pricing groups from the games pricing setup source', () => {
    const { page } = createPage();

    expect(page.game()?.betOptionGroups).toEqual(pricingGroups());
    expect(page.pricingGroups()).toEqual(pricingGroups());
  });

  it('reloads both game settings and pricing setup details', () => {
    const { page, gamesResource, pricingResource } = createPage();

    page.reload();

    expect(gamesResource.reload).toHaveBeenCalled();
    expect(pricingResource.reload).toHaveBeenCalled();
  });

  // --- POS visibility toggle ---

  it('toggleOptionEnabled disables POS visibility as a side-effect when unchecking', () => {
    const { page } = createPage();
    page.toggleOptionEnabled({ betType: 'MARRIAGE_2D2D', optionCode: 2, checked: false });
    const option = page.betOptionConfig()?.betTypes[0].options.find(o => o.code === 2);
    expect(option?.enabled).toBe(false);
    expect(option?.visibleInPos).toBe(false);
  });

  it('toggleOptionVisibleInPos changes POS visibility without affecting enabled state', () => {
    const { page } = createPage();
    page.toggleOptionVisibleInPos({ betType: 'MARRIAGE_2D2D', optionCode: 1, checked: false });
    const option = page.betOptionConfig()?.betTypes[0].options.find(o => o.code === 1);
    expect(option?.visibleInPos).toBe(false);
    expect(option?.enabled).toBe(true);
  });

  // --- Game activation state ---

  it('game readyForSale is false when pricing readiness is not READY', () => {
    const { page } = createPage({ pricingReadiness: 'MISSING_REQUIRED_PAYOUT' });
    expect(page.game()?.readyForSale).toBe(false);
  });

  // --- Stake min/max validation ---

  it('stakeErrorKey is null for valid stakes at page load', () => {
    const { page } = createPage();
    expect(page.stakeErrorKey()).toBeNull();
  });

  it('stakeErrorKey returns maxAfterMin when minStake exceeds maxStake', () => {
    const { page } = createPage();
    page.model.update(m => ({ ...m, minStake: 5000, maxStake: 100 }));
    expect(page.stakeErrorKey()).toBe('admin.games.settings.stakes.error.maxAfterMin');
  });

  it('stakeErrorKey returns minPositive for zero minStake', () => {
    const { page } = createPage();
    page.model.update(m => ({ ...m, minStake: 0 }));
    expect(page.stakeErrorKey()).toBe('admin.games.settings.stakes.error.minPositive');
  });

  it('stakeErrorKey returns maxPositive for negative maxStake', () => {
    const { page } = createPage();
    page.model.update(m => ({ ...m, minStake: 1, maxStake: -10 }));
    expect(page.stakeErrorKey()).toBe('admin.games.settings.stakes.error.maxPositive');
  });

  // --- Availability times excluded when disabled ---

  it('submit strips availability times when availabilityEnabled is false', () => {
    const { page, gamesApi } = createPage();
    page.model.update(m => ({
      ...m,
      availabilityEnabled: false,
      startLocalTime: '08:00',
      endLocalTime: '20:00',
    }));
    page.submit(new Event('submit'));
    expect(gamesApi.updateGameSettings).toHaveBeenCalledWith(
      'HT_MARYAJ_GRATIS',
      expect.objectContaining({ startLocalTime: null, endLocalTime: null }),
      expect.anything(),
    );
  });

  // --- Payout edit flow ---

  it('updatePricingOdds sets odds on the targeted variant', () => {
    const { page } = createPage();
    page.updatePricingOdds({ groupId: 'maryaj', pricingVariantCode: 'MARYAJ_EXACT', rawValue: '750' });
    const variant = page.pricingGroups()[0]?.variants.find(v => v.pricingVariantCode === 'MARYAJ_EXACT');
    expect(variant?.odds).toBe(750);
    expect(variant?.payoutRuleType).toBe('STAKE_MULTIPLIER');
  });

  it('updatePricingOdds ignores non-positive values and leaves variant unchanged', () => {
    const { page } = createPage();
    page.updatePricingOdds({ groupId: 'maryaj', pricingVariantCode: 'MARYAJ_EXACT', rawValue: '-5' });
    const variant = page.pricingGroups()[0]?.variants.find(v => v.pricingVariantCode === 'MARYAJ_EXACT');
    expect(variant?.odds).toBe(500);
  });

  // --- Payout delete confirmation dialog ---

  it('requestClearPricingOdds opens a confirmation dialog before deleting', () => {
    const { page } = createPage();
    const dialog = TestBed.inject(MatDialog);
    page.requestClearPricingOdds({ groupId: 'maryaj', pricingVariantCode: 'MARYAJ_EXACT', label: 'Egzak' });
    expect(dialog.open).toHaveBeenCalled();
  });

  it('requestClearPricingOdds clears odds and fixedAmount after confirmation', () => {
    const { page } = createPage();
    page.requestClearPricingOdds({ groupId: 'maryaj', pricingVariantCode: 'MARYAJ_EXACT', label: 'Egzak' });
    const variant = page.pricingGroups()[0]?.variants.find(v => v.pricingVariantCode === 'MARYAJ_EXACT');
    expect(variant?.odds).toBeNull();
    expect(variant?.fixedAmount).toBeNull();
  });

  // --- Dirty state and cancel behavior ---

  it('dirty is false before any edits', () => {
    const { page } = createPage();
    expect(page.dirty()).toBe(false);
  });

  it('dirty is true after modifying the model', () => {
    const { page } = createPage();
    page.model.update(m => ({ ...m, displayName: 'Changed Name' }));
    expect(page.dirty()).toBe(true);
  });

  it('dirty is true after modifying pricing groups', () => {
    const { page } = createPage();
    page.updatePricingOdds({ groupId: 'maryaj', pricingVariantCode: 'MARYAJ_EXACT', rawValue: '999' });
    expect(page.dirty()).toBe(true);
  });

  it('requestCancel navigates directly when not dirty', () => {
    const { page, router } = createPage();
    const dialog = TestBed.inject(MatDialog);
    page.requestCancel();
    expect(dialog.open).not.toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalled();
  });

  it('requestCancel opens discard dialog when dirty and navigates on confirmation', () => {
    const { page, router } = createPage();
    const dialog = TestBed.inject(MatDialog);
    page.model.update(m => ({ ...m, displayName: 'Changed' }));
    page.requestCancel();
    expect(dialog.open).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalled();
  });
});

function createPage(options: { queryParams?: Record<string, string>; pricingReadiness?: string } = {}) {
  const gamesResource = resource([game()]);
  const betOptionResource = resource(betOptionConfig());
  const readiness = options.pricingReadiness ?? 'READY';
  const pricingResource = resource([{
    ...pricingGame(),
    readiness: { status: readiness, label: `admin.gamesPricing.readiness.${readiness}`, reason: null },
  }]);
  const gamesApi = {
    listEnabledGamesResource: vi.fn(() => gamesResource),
    getBetOptionConfigResource: vi.fn(() => betOptionResource),
    updateGameSettings: vi.fn(() => of(undefined)),
    updateBetOptionConfig: vi.fn(() => of(undefined)),
  };
  const pricingApi = {
    getGamesPricingResource: vi.fn(() => pricingResource),
    upsertTenantOdds: vi.fn(() => of(undefined)),
    deleteTenantOdds: vi.fn(() => of(undefined)),
  };
  const router = { navigate: vi.fn() };
  const paramMap = convertToParamMap({ gameCode: 'HT_MARYAJ_GRATIS' });
  const queryParamMap = convertToParamMap(options.queryParams ?? {});

  TestBed.configureTestingModule({
    providers: [
      {
        provide: ActivatedRoute,
        useValue: {
          paramMap: of(paramMap),
          queryParamMap: of(queryParamMap),
          snapshot: { paramMap, queryParamMap },
        },
      },
      { provide: GamesAdminApiService, useValue: gamesApi },
      { provide: AdminGamesPricingApiService, useValue: pricingApi },
      { provide: Router, useValue: router },
      {
        provide: MatDialog,
        useValue: {
          open: vi.fn(() => ({ afterClosed: () => of({ confirmed: true }) })),
        },
      },
      {
        provide: TranslateService,
        useValue: { instant: (key: string) => key },
      },
    ],
  });

  const page = TestBed.runInInjectionContext(() => new AdminGameSettingsPage());
  TestBed.flushEffects();
  return { page, gamesApi, pricingApi, router, gamesResource, pricingResource };
}

function resource<T>(initialValue: T) {
  const current = signal(initialValue);
  return {
    status: vi.fn(() => 'resolved'),
    value: vi.fn(() => current()),
    error: vi.fn(() => null),
    reload: vi.fn(),
    update: vi.fn((updater: (value: T) => T) => {
      current.update(updater);
    }),
  };
}

function game(): TenantGameView {
  return {
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
}

function pricingGame(): TenantGamePricingView {
  return {
    gameCode: 'HT_MARYAJ_GRATIS',
    tenantGameId: 'tenant-game-maryaj-gratis',
    gameName: 'Maryaj gratis',
    catalogStatus: 'AVAILABLE',
    tenantStatus: 'ACTIVE',
    visibleInPos: true,
    pricingProfileLabel: 'admin.gamesPricing.card.standardPricingProfile',
    odds: pricingGroups().flatMap(group => group.variants),
    oddsGroups: pricingGroups(),
    limits: {
      minStake: 1,
      maxStake: 10_000_000,
      maxPerDraw: null,
      currency: 'HTG',
    },
    readiness: {
      status: 'READY',
      label: 'admin.gamesPricing.readiness.READY',
      reason: null,
    },
  };
}

function betOptionConfig(): TenantGameBetOptionConfigView {
  return {
    gameCode: 'HT_MARYAJ_GRATIS',
    betTypes: [
      {
        betType: 'MARRIAGE_2D2D',
        selectionPolicy: 'EXPLICIT_ONLY',
        defaultOption: 1,
        options: [
          {
            code: 1,
            label: 'Egzak',
            description: null,
            enabled: true,
            visibleInPos: true,
            displayOrder: 1,
          },
          {
            code: 2,
            label: 'Reverse',
            description: null,
            enabled: true,
            visibleInPos: true,
            displayOrder: 2,
          },
        ],
      },
    ],
  };
}

function pricingGroups(): readonly TenantGameOddGroupView[] {
  return [
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
        {
          label: 'Reverse',
          value: '250',
          odds: 250,
          payoutRuleType: 'STAKE_MULTIPLIER',
          fixedAmount: null,
          betType: 'MARRIAGE_2D2D',
          betOption: 2,
          pricingVariantCode: 'MARYAJ_REVERSE',
        },
      ],
    },
  ];
}
