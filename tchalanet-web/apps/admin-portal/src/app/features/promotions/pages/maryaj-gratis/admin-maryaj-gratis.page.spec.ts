import { ViewportScroller } from '@angular/common';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { TenantGamePricingView } from '../../../games-pricing/data-access/admin-games-pricing.models';
import { AdminMaryajGratisStore } from './admin-maryaj-gratis.store';
import { AdminMaryajGratisPage } from './admin-maryaj-gratis.page';

describe(AdminMaryajGratisPage.name, () => {
  let dialog: { open: ReturnType<typeof vi.fn> };
  let router: { navigate: ReturnType<typeof vi.fn> };
  let store: { load: ReturnType<typeof vi.fn>; state: ReturnType<typeof vi.fn> };
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
      load: vi.fn(),
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
