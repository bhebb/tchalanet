export type TenantStatus = 'DRAFT' | 'ACTIVE' | 'SUSPENDED' | 'REJECTED' | 'ARCHIVED';
export type TenantType = 'BORLETTE' | 'RESEAU' | 'AMBULANT';
export type TenantProvisioningProfile = 'MINIMAL' | 'DEFAULT_HAITI_LOTTERY' | 'DEMO';
export type TenantReadinessStatus = 'READY' | 'INCOMPLETE' | 'BLOCKED' | 'MISSING' | 'UNKNOWN';

export interface TenantReadinessSectionView {
  readonly id: string;
  readonly labelKey: string;
  readonly status: string;
  readonly route: string;
  readonly issues: readonly unknown[];
}

export interface TenantReadinessView {
  readonly status: TenantReadinessStatus;
  readonly missingCount: number;
  readonly sections: readonly TenantReadinessSectionView[];
}

export interface PlanSummaryView {
  readonly id: string;
  readonly code: string;
  readonly name: string;
  readonly description?: string | null;
  readonly priceAmount?: number | null;
  readonly currency?: string | null;
  readonly billingPeriod?: string | null;
  readonly active: boolean;
  readonly isDefault: boolean;
}

export type SubscriptionStatus =
  | 'ACTIVE'
  | 'TRIAL'
  | 'SUSPENDED'
  | 'CANCELED'
  | 'EXPIRED';

export interface SubscriptionView {
  readonly tenantId: string;
  readonly planCode: string;
  readonly status: SubscriptionStatus;
  readonly startedAt: string;
  readonly endsAt?: string | null;
  readonly version: number;
  readonly updatedAt: string;
}

export interface ApplyPlanResult {
  readonly subscriptionId: string;
  readonly status: SubscriptionStatus;
}
