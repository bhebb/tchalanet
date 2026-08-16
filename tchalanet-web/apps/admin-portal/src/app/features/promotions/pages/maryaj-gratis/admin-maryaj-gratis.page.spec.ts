import { ViewportScroller } from '@angular/common';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { TenantGamePricingView } from '../../../games-pricing/data-access/admin-games-pricing.models';
import { PromotionCampaignView } from '../../data-access/admin-promotions-api.service';
import { AdminMaryajGratisStore } from './admin-maryaj-gratis.store';
import { AdminMaryajGratisPage } from './admin-maryaj-gratis.page';

describe(AdminMaryajGratisPage.name, () => {
  let dialog: { open: ReturnType<typeof vi.fn> };
  let router: { navigate: ReturnType<typeof vi.fn> };
  let store: {
    disableGame: ReturnType<typeof vi.fn>;
    load: ReturnType<typeof vi.fn>;
    maryajGame: ReturnType<typeof vi.fn>;
    pause: ReturnType<typeof vi.fn>;
    state: ReturnType<typeof vi.fn>;
  };
  let page: AdminMaryajGratisPage;

  beforeEach(() => {
    dialog = {
      open: vi.fn(() => ({
        afterClosed: () => of(false),
      })),
    };
    router = {
      navigate: vi.fn(),
    };
    store = {
      disableGame: vi.fn(),
      load: vi.fn(),
      maryajGame: vi.fn(() => maryajGame()),
      pause: vi.fn(),
      state: vi.fn(() => 'ready'),
    };
  });

  function configurePage(queryParams: Record<string, string> = {}): void {
    const queryParamMap = convertToParamMap(queryParams);
    TestBed.configureTestingModule({
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            fragment: of(null),
            queryParamMap: of(queryParamMap),
            snapshot: { fragment: null, queryParamMap },
          },
        },
        { provide: ViewportScroller, useValue: { scrollToAnchor: vi.fn() } },
        { provide: MatDialog, useValue: dialog },
        { provide: Router, useValue: router },
        { provide: TranslateService, useValue: { instant: (key: string) => key } },
        { provide: AdminMaryajGratisStore, useValue: store },
      ],
    });
    page = TestBed.runInInjectionContext(() => new AdminMaryajGratisPage());
  }

  it('navigates to the shared routed game editor for Maryaj game configuration', () => {
    configurePage();

    page.openGameSettings(maryajGame());

    expect(router.navigate).toHaveBeenCalledWith(
      ['/app/admin/games', 'HT_MARYAJ_GRATIS', 'settings'],
      { queryParams: { returnTo: 'maryaj-gratis' } },
    );
  });

  it('preserves setup navigation context when opening the routed game editor', () => {
    configurePage({ from: 'setup' });

    page.openGameSettings(maryajGame());

    expect(router.navigate).toHaveBeenCalledWith(
      ['/app/admin/games', 'HT_MARYAJ_GRATIS', 'settings'],
      { queryParams: { returnTo: 'maryaj-gratis', from: 'setup' } },
    );
  });

  it('confirms before disabling Maryaj Gratis from the shared game card', () => {
    dialog.open.mockReturnValueOnce({
      afterClosed: () => of({ confirmed: true }),
    });
    configurePage();

    page.confirmDisableGame('HT_MARYAJ_GRATIS');

    expect(dialog.open).toHaveBeenCalledWith(
      expect.any(Function),
      expect.objectContaining({
        data: expect.objectContaining({
          title: 'admin.gamesPricing.confirm.disableTitle',
          message: 'admin.gamesPricing.confirm.disableMessage',
          confirmLabel: 'admin.gamesPricing.confirm.disableAction',
          destructive: true,
        }),
      }),
    );
    expect(store.disableGame).toHaveBeenCalledWith('HT_MARYAJ_GRATIS');
  });

  it('confirms before pausing an offer campaign from the offer card', () => {
    dialog.open.mockReturnValueOnce({
      afterClosed: () => of({ confirmed: true }),
    });
    configurePage();

    page.confirmPauseOffer(campaign());

    expect(dialog.open).toHaveBeenCalledWith(
      expect.any(Function),
      expect.objectContaining({
        data: expect.objectContaining({
          title: 'admin.maryajGratis.offer.confirmPause.title',
          destructive: true,
        }),
      }),
    );
    expect(store.pause).toHaveBeenCalledWith(campaign());
  });

  function campaign(overrides: Partial<PromotionCampaignView> = {}): PromotionCampaignView {
    return {
      id: { value: 'campaign-maryaj-gratis' },
      code: 'DEFAULT_MARYAJ_GRATIS',
      name: 'Maryaj gratis',
      status: 'ACTIVE',
      priority: 100,
      startsAt: '2026-07-01T00:00:00Z',
      endsAt: '2036-07-01T23:59:59Z',
      rules: [],
      ...overrides,
    };
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
        label: 'Pare',
        reason: null,
      },
      ...overrides,
    };
  }
});
