import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';

import { AdminGamesPricingApiService } from '../../data-access/admin-games-pricing-api.service';
import { TenantGameOddGroupView } from '../../data-access/admin-games-pricing.models';
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
});

function createPage(options: { queryParams?: Record<string, string> } = {}) {
  const gamesResource = resource([game()]);
  const betOptionResource = resource(betOptionConfig());
  const gamesApi = {
    listEnabledGamesResource: vi.fn(() => gamesResource),
    getBetOptionConfigResource: vi.fn(() => betOptionResource),
    updateGameSettings: vi.fn(() => of(undefined)),
    updateBetOptionConfig: vi.fn(() => of(undefined)),
  };
  const pricingApi = {
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
  return { page, gamesApi, pricingApi, router };
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
    betOptionGroups: pricingGroups(),
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
