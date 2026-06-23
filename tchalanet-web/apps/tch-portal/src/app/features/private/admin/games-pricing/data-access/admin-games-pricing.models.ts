export type CatalogStatus = 'AVAILABLE' | 'DISABLED' | 'COMING_SOON';
export type TenantGameStatus = 'ACTIVE' | 'INACTIVE' | 'NEEDS_CONFIG' | 'UNAVAILABLE';
export type ReadinessStatus = 'READY' | 'TODO' | 'BLOCKED';

export interface TenantGameOddView {
  readonly label: string;
  readonly value: string;
}

export interface TenantGameLimitView {
  readonly minStake: number | null;
  readonly maxStake: number | null;
  readonly maxPerDraw: number | null;
  readonly currency: string;
}

export interface TenantGamePricingView {
  readonly gameCode: string;
  readonly gameName: string;
  readonly catalogStatus: CatalogStatus;
  readonly tenantStatus: TenantGameStatus;
  readonly pricingProfileLabel: string | null;
  readonly odds: readonly TenantGameOddView[];
  readonly limits: TenantGameLimitView;
  readonly readiness: {
    readonly status: ReadinessStatus;
    readonly label: string;
    readonly reason: string | null;
  };
}
