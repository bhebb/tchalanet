import { ViewportScroller } from '@angular/common';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { TenantGamePricingView } from '../../../games-pricing/data-access/admin-games-pricing.models';
import { GameSettingsDialog } from '../../../games-pricing/components/dialogs/game-settings.dialog';
import { AdminMaryajGratisStore } from './admin-maryaj-gratis.store';
import { AdminMaryajGratisPage } from './admin-maryaj-gratis.page';

describe(AdminMaryajGratisPage.name, () => {
  let dialog: { open: ReturnType<typeof vi.fn> };
  let store: { load: ReturnType<typeof vi.fn>; state: ReturnType<typeof vi.fn> };
  let page: AdminMaryajGratisPage;

  beforeEach(() => {
    dialog = {
      open: vi.fn(() => ({
        afterClosed: () => of(false),
      })),
    };
    store = {
      load: vi.fn(),
      state: vi.fn(() => 'ready'),
    };

    TestBed.configureTestingModule({
      providers: [
        { provide: ActivatedRoute, useValue: { fragment: of(null), snapshot: { fragment: null } } },
        { provide: ViewportScroller, useValue: { scrollToAnchor: vi.fn() } },
        { provide: MatDialog, useValue: dialog },
        { provide: TranslateService, useValue: { instant: (key: string) => key } },
        { provide: AdminMaryajGratisStore, useValue: store },
      ],
    });
    page = TestBed.runInInjectionContext(() => new AdminMaryajGratisPage());
  });

  it('opens the shared game editor with Maryaj game configuration values', () => {
    const betOptions = [
      {
        label: 'Exact',
        value: 'EXACT',
        odds: null,
        payoutRuleType: 'FIXED_AMOUNT' as const,
        fixedAmount: 500,
        betType: 'MARYAJ',
        betOption: 1,
        pricingVariantCode: 'MARRIAGE_EXACT_ORDER',
      },
    ];
    const game = maryajGame({
      tenantStatus: 'NEEDS_CONFIG',
      visibleInPos: false,
      readiness: {
        status: 'TODO',
        label: 'Pou konfigire',
        reason: 'missing pricing',
      },
      limits: {
        minStake: 5,
        maxStake: 5000,
        maxPerDraw: null,
        currency: 'HTG',
      },
      odds: betOptions,
    });

    page.openGameSettings(game);

    expect(dialog.open).toHaveBeenCalledWith(
      GameSettingsDialog,
      expect.objectContaining({
        data: expect.objectContaining({
          game: expect.objectContaining({
            gameCode: 'HT_MARYAJ_GRATIS',
            enabled: true,
            visibleInPos: false,
            minStake: 5,
            maxStake: 5000,
            readyForSale: false,
            betOptions,
          }),
        }),
        width: 'min(48rem, 100vw)',
        maxWidth: '100vw',
        height: 'min(54rem, 100dvh)',
        maxHeight: '100dvh',
      }),
    );
  });

  it('reloads Maryaj Gratis data after the shared game editor saves changes', () => {
    dialog.open.mockReturnValueOnce({
      afterClosed: () => of(true),
    });

    page.openGameSettings(maryajGame());

    expect(store.load).toHaveBeenCalledOnce();
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
