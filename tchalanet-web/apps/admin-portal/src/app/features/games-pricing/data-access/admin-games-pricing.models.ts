import type {
  ConsoleCatalogStatus,
  ConsoleReadinessStatus,
  ConsoleTenantGameStatus,
} from '@tch/web/console';

export type CatalogStatus = ConsoleCatalogStatus;
export type TenantGameStatus = ConsoleTenantGameStatus;
export type ReadinessStatus = Extract<ConsoleReadinessStatus, 'READY' | 'TODO' | 'BLOCKED'>;

export interface TenantGameOddView {
  readonly label: string;
  readonly value: string;
  readonly odds: number | null;
  readonly payoutRuleType: 'STAKE_MULTIPLIER' | 'FIXED_AMOUNT';
  readonly fixedAmount: number | null;
  readonly betType: string;
  readonly betOption: number | null;
  readonly pricingVariantCode: string | null;
}

export interface TenantGameOddGroupView {
  readonly id: string;
  readonly label: string;
  readonly betType: string;
  readonly betOption: number | null;
  readonly variants: readonly TenantGameOddView[];
}

export interface TenantGameLimitView {
  readonly minStake: number | null;
  readonly maxStake: number | null;
  readonly maxPerDraw: number | null;
  readonly currency: string;
}

export interface TenantGamePricingView {
  readonly gameCode: string;
  readonly tenantGameId: string | null;
  readonly gameName: string;
  readonly catalogStatus: CatalogStatus;
  readonly tenantStatus: TenantGameStatus;
  readonly pricingProfileLabel: string | null;
  readonly odds: readonly TenantGameOddView[];
  readonly oddsGroups: readonly TenantGameOddGroupView[];
  readonly limits: TenantGameLimitView;
  readonly readiness: {
    readonly status: ReadinessStatus;
    readonly label: string;
    readonly reason: string | null;
  };
}
