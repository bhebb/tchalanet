import type { TenantGamePricingView } from './admin-games-pricing.models';
import type { TenantGameView } from './games-admin-api.service';

export function toTenantGameSettingsView(game: TenantGamePricingView): TenantGameView {
  return {
    gameCode: game.gameCode,
    catalogName: game.gameName,
    displayName: game.gameName,
    category: null,
    enabled: game.tenantStatus === 'ACTIVE' || game.tenantStatus === 'NEEDS_CONFIG',
    visibleInPos: game.visibleInPos,
    displayOrder: 0,
    minStake: game.limits.minStake,
    maxStake: game.limits.maxStake,
    availabilityEnabled: false,
    availabilityDays: null,
    startLocalTime: null,
    endLocalTime: null,
    readyForSale: game.readiness.status === 'READY',
    betOptions: game.odds,
    betOptionGroups: game.oddsGroups,
  };
}
