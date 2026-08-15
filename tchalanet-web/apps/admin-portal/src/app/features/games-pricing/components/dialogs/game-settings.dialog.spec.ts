import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { AdminGamesPricingApiService } from '../../data-access/admin-games-pricing-api.service';
import { GamesAdminApiService, TenantGameView } from '../../data-access/games-admin-api.service';
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

  function translateInstant(key: string): string {
    const translations: Record<string, string> = {
      'common.not_available': 'Pa disponib',
    };
    return translations[key] ?? key;
  }
});
