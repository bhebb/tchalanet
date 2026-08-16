import { describe, expect, it } from 'vitest';

import type { TenantGamePricingView } from './admin-games-pricing.models';
import { toTenantGameSettingsView } from './tenant-game-settings-adapter';

describe(toTenantGameSettingsView.name, () => {
  it('maps tenant game pricing rows to the shared game settings surface model', () => {
    const odds = [
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
    const oddsGroups = [
      {
        id: 'maryaj',
        label: 'Maryaj',
        betType: 'MARYAJ',
        betOption: null,
        variants: odds,
      },
    ];

    expect(
      toTenantGameSettingsView(
        game({
          gameCode: 'HT_MARYAJ_GRATIS',
          gameName: 'Maryaj gratis',
          tenantStatus: 'NEEDS_CONFIG',
          visibleInPos: false,
          limits: {
            minStake: 5,
            maxStake: 5000,
            maxPerDraw: null,
            currency: 'HTG',
          },
          odds,
          oddsGroups,
          readiness: {
            status: 'TODO',
            label: 'Pou konfigire',
            reason: 'missing pricing',
          },
        }),
      ),
    ).toEqual({
      gameCode: 'HT_MARYAJ_GRATIS',
      catalogName: 'Maryaj gratis',
      displayName: 'Maryaj gratis',
      category: null,
      enabled: true,
      visibleInPos: false,
      displayOrder: 0,
      minStake: 5,
      maxStake: 5000,
      availabilityEnabled: false,
      availabilityDays: null,
      startLocalTime: null,
      endLocalTime: null,
      readyForSale: false,
      betOptions: odds,
      betOptionGroups: oddsGroups,
    });
  });

  it('marks active ready games as enabled and ready for sale', () => {
    expect(toTenantGameSettingsView(game()).enabled).toBe(true);
    expect(toTenantGameSettingsView(game()).readyForSale).toBe(true);
  });

  it('keeps inactive games disabled for the settings surface', () => {
    expect(toTenantGameSettingsView(game({ tenantStatus: 'INACTIVE' })).enabled).toBe(false);
  });

  function game(overrides: Partial<TenantGamePricingView> = {}): TenantGamePricingView {
    return {
      gameCode: 'HT_BOLET',
      tenantGameId: 'tenant-game-1',
      gameName: 'Bolèt',
      catalogStatus: 'AVAILABLE',
      tenantStatus: 'ACTIVE',
      visibleInPos: true,
      pricingProfileLabel: null,
      odds: [],
      oddsGroups: [],
      limits: {
        minStake: 1,
        maxStake: 1000,
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
