import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { AdminGamesPricingApiService } from '../../data-access/admin-games-pricing-api.service';
import {
  GamesAdminApiService,
  TenantGameBetOptionConfigView,
  TenantGameView,
} from '../../data-access/games-admin-api.service';
import { GameSettingsDialog } from './game-settings.dialog';

describe(GameSettingsDialog.name, () => {
  let component: GameSettingsDialog;
  let gamesApi: {
    getBetOptionConfig: ReturnType<typeof vi.fn>;
    updateGameSettings: ReturnType<typeof vi.fn>;
    updateBetOptionConfig: ReturnType<typeof vi.fn>;
  };
  let pricingApi: {
    upsertTenantOdds: ReturnType<typeof vi.fn>;
    deleteTenantOdds: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    gamesApi = {
      getBetOptionConfig: vi.fn(() => of({ gameCode: 'HT_MARYAJ_GRATIS', betTypes: [] })),
      updateGameSettings: vi.fn(() => of(undefined)),
      updateBetOptionConfig: vi.fn(() => of(undefined)),
    };
    pricingApi = {
      upsertTenantOdds: vi.fn(() => of(undefined)),
      deleteTenantOdds: vi.fn(() => of(undefined)),
    };

    TestBed.configureTestingModule({
      providers: [
        { provide: MAT_DIALOG_DATA, useValue: { game: game() } },
        { provide: MatDialogRef, useValue: { close: vi.fn() } },
        { provide: GamesAdminApiService, useValue: gamesApi },
        { provide: AdminGamesPricingApiService, useValue: pricingApi },
        { provide: TranslateService, useValue: { instant: translateInstant } },
      ],
    });

    component = TestBed.runInInjectionContext(() => new GameSettingsDialog());
  });

  it('formats stake amounts with readable grouping for field hints', () => {
    expect(component.formattedStakeAmount(10_000_000)).toBe('10 000 000 HTG');
    expect(component.formattedStakeAmount(null)).toBe('Pa disponib');
  });

  it('validates stake min and max close to the game editor fields', () => {
    expect(component.stakeErrorKeyFor(0, 100)).toBe(
      'admin.games.settings.stakes.error.minPositive',
    );
    expect(component.stakeErrorKeyFor(10, 0)).toBe(
      'admin.games.settings.stakes.error.maxPositive',
    );
    expect(component.stakeErrorKeyFor(500, 100)).toBe(
      'admin.games.settings.stakes.error.maxAfterMin',
    );
    expect(component.stakeErrorKeyFor(100, 500)).toBeNull();
  });

  it('does not save game settings when stake limits are invalid', () => {
    component.model.update(value => ({ ...value, minStake: 500, maxStake: 100 }));

    component.submit(new Event('submit'));

    expect(gamesApi.updateGameSettings).not.toHaveBeenCalled();
  });

  it('saves POS visibility without changing game activation', () => {
    component.model.update(value => ({ ...value, visibleInPos: false }));

    component.submit(new Event('submit'));

    expect(gamesApi.updateGameSettings).toHaveBeenCalledWith(
      'HT_MARYAJ_GRATIS',
      expect.objectContaining({
        visibleInPos: false,
      }),
      { suppressShellFeedback: true },
    );
  });

  it('updates Exact and Reverse option controls independently', () => {
    component.betOptionConfig.set(maryajBetOptionConfig());

    component.toggleOptionVisibleInPos('MARRIAGE_2D2D', 1, false);
    component.toggleOptionEnabled('MARRIAGE_2D2D', 2, false);

    expect(component.betOptionConfig()?.betTypes[0]?.options).toEqual([
      expect.objectContaining({
        code: 1,
        enabled: true,
        visibleInPos: false,
      }),
      expect.objectContaining({
        code: 2,
        enabled: false,
        visibleInPos: false,
      }),
    ]);
  });

  it('saves supported option policy and default selection', () => {
    component.betOptionConfig.set(maryajBetOptionConfig());

    component.updateSelectionPolicy('MARRIAGE_2D2D', 'EXPLICIT_WITH_AUTO_OPTION');
    component.updateDefaultOption('MARRIAGE_2D2D', 1);
    component.submit(new Event('submit'));

    expect(gamesApi.updateBetOptionConfig).toHaveBeenCalledWith(
      'HT_MARYAJ_GRATIS',
      {
        betTypes: [
          {
            betType: 'MARRIAGE_2D2D',
            selectionPolicy: 'EXPLICIT_WITH_AUTO_OPTION',
            defaultOption: 1,
            options: [
              {
                code: 1,
                enabled: true,
                visibleInPos: true,
                displayOrder: 1,
              },
              {
                code: 2,
                enabled: true,
                visibleInPos: true,
                displayOrder: 2,
              },
            ],
          },
        ],
      },
      { suppressShellFeedback: true },
    );
  });

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

  function maryajBetOptionConfig(): TenantGameBetOptionConfigView {
    return {
      gameCode: 'HT_MARYAJ_GRATIS',
      betTypes: [
        {
          betType: 'MARRIAGE_2D2D',
          selectionPolicy: 'EXPLICIT_ONLY',
          defaultOption: null,
          options: [
            {
              code: 1,
              label: 'Exact',
              description: 'Backend exact description',
              enabled: true,
              visibleInPos: true,
              displayOrder: 1,
            },
            {
              code: 2,
              label: 'Reverse',
              description: 'Backend reverse description',
              enabled: true,
              visibleInPos: true,
              displayOrder: 2,
            },
          ],
        },
      ],
    };
  }

  function translateInstant(key: string): string {
    const translations: Record<string, string> = {
      'common.not_available': 'Pa disponib',
    };
    return translations[key] ?? key;
  }
});
