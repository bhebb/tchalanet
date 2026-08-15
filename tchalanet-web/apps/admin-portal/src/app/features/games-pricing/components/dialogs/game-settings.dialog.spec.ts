import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { Subject, of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { AdminGamesPricingApiService } from '../../data-access/admin-games-pricing-api.service';
import { TenantGameOddGroupView } from '../../data-access/admin-games-pricing.models';
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
  let dialog: {
    open: ReturnType<typeof vi.fn>;
  };
  let dialogRef: {
    close: ReturnType<typeof vi.fn>;
    backdropClick: ReturnType<typeof vi.fn>;
    keydownEvents: ReturnType<typeof vi.fn>;
    disableClose?: boolean;
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
    dialog = {
      open: vi.fn(() => ({ afterClosed: () => of({ confirmed: true }) })),
    };
    dialogRef = {
      close: vi.fn(),
      backdropClick: vi.fn(() => new Subject<MouseEvent>()),
      keydownEvents: vi.fn(() => new Subject<KeyboardEvent>()),
    };

    TestBed.configureTestingModule({
      providers: [
        { provide: MAT_DIALOG_DATA, useValue: { game: game() } },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatDialog, useValue: dialog },
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

  it('prevents silent dialog close so unsaved changes can be handled', () => {
    expect(dialogRef.disableClose).toBe(true);
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

  it('closes cancel immediately when no game settings changed', () => {
    component.requestCancel();

    expect(dialog.open).not.toHaveBeenCalled();
    expect(dialogRef.close).toHaveBeenCalledWith(undefined);
  });

  it('confirms before discarding unsaved game settings', () => {
    component.model.update(value => ({ ...value, visibleInPos: false }));

    component.requestCancel();

    expect(dialog.open).toHaveBeenCalled();
    expect(dialogRef.close).toHaveBeenCalledWith(undefined);
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

  it('opens one payout rule editor and summarizes the current rule', () => {
    const [group] = maryajPricingGroups();
    const [variant] = group.variants;
    component.pricingGroups.set([group]);

    component.editPricingVariant(group.id, variant.pricingVariantCode, variant.label);

    expect(
      component.isPricingVariantEditing(group.id, variant.pricingVariantCode, variant.label),
    ).toBe(true);
    expect(component.pricingRuleSummary(variant)).toBe('Montan fiks: 500 HTG');
  });

  it('saves payout rule edits', () => {
    const [group] = maryajPricingGroups();
    const [variant] = group.variants;
    component.pricingGroups.set([group]);

    component.updatePricingRuleType(group.id, variant.pricingVariantCode, 'STAKE_MULTIPLIER');
    component.updatePricingOdds(group.id, variant.pricingVariantCode, '4.5');
    component.submit(new Event('submit'));

    expect(pricingApi.upsertTenantOdds).toHaveBeenCalledWith(
      {
        gameCode: 'HT_MARYAJ_GRATIS',
        pricingVariantCode: 'MARYAJ_EXACT',
        betType: 'MARRIAGE_2D2D',
        betOption: 1,
        odds: 4.5,
        payoutRuleType: 'STAKE_MULTIPLIER',
        fixedAmount: null,
      },
      { suppressShellFeedback: true },
    );
  });

  it('confirms before clearing a payout rule', () => {
    const [group] = maryajPricingGroups();
    const [variant] = group.variants;
    component.pricingGroups.set([group]);

    component.requestClearPricingOdds(group.id, variant.pricingVariantCode, variant.label);

    expect(dialog.open).toHaveBeenCalled();
    component.submit(new Event('submit'));

    expect(component.pricingGroups()[0]?.variants[0]).toEqual(
      expect.objectContaining({
        odds: null,
        fixedAmount: null,
      }),
    );
    expect(pricingApi.deleteTenantOdds).toHaveBeenCalledWith(
      {
        gameCode: 'HT_MARYAJ_GRATIS',
        pricingVariantCode: 'MARYAJ_EXACT',
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

  function maryajPricingGroups(): readonly TenantGameOddGroupView[] {
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
            odds: null,
            payoutRuleType: 'FIXED_AMOUNT',
            fixedAmount: 500,
            betType: 'MARRIAGE_2D2D',
            betOption: 1,
            pricingVariantCode: 'MARYAJ_EXACT',
          },
        ],
      },
    ];
  }

  function translateInstant(key: string, params?: Record<string, unknown>): string {
    const translations: Record<string, string> = {
      'common.not_available': 'Pa disponib',
      'admin.games.settings.payouts.fixedAmountSummary': 'Montan fiks: {{amount}}',
      'admin.games.settings.payouts.multiplierSummary': 'Miltiplikatè: ×{{odds}}',
      'admin.games.settings.payouts.notConfigured': 'Pa konfigire',
      'admin.games.settings.payouts.confirmClearTitle': 'Efase règ {{label}} la?',
      'admin.games.settings.payouts.confirmClearMessage':
        'Règ barèm sa a ap vin pa konfigire jiskaske ou anrejistre yon nouvo valè.',
      'admin.games.settings.payouts.confirmClearAction': 'Efase règ la',
      'admin.games.settings.confirmDiscard.title': 'Kite san anrejistre?',
      'admin.games.settings.confirmDiscard.message': 'Chanjman ou fè yo p ap anrejistre.',
      'admin.games.settings.confirmDiscard.action': 'Kite san anrejistre',
      'common.cancel': 'Anile',
    };
    return interpolate(translations[key] ?? key, params);
  }

  function interpolate(value: string, params?: Record<string, unknown>): string {
    if (!params) return value;
    return Object.entries(params).reduce(
      (acc, [key, replacement]) => acc.replace(`{{${key}}}`, `${replacement}`),
      value,
    );
  }
});
