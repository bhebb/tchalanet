import type {
  ConsoleReadinessStatus,
  ConsoleSubscriptionStatus,
  ConsoleTenantProvisioningProfile,
  ConsoleTenantStatus,
  ConsoleTenantType,
} from '@tch/web/console';

export type TenantStatus = ConsoleTenantStatus;
export type TenantType = ConsoleTenantType;
export type TenantProvisioningProfile = ConsoleTenantProvisioningProfile;
export type TenantReadinessStatus = Extract<
  ConsoleReadinessStatus,
  'READY' | 'INCOMPLETE' | 'BLOCKED' | 'MISSING' | 'UNKNOWN'
>;

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

export type SubscriptionStatus = ConsoleSubscriptionStatus;

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

export interface TenantCapabilitySnapshot {
  readonly tenantId: string;
  readonly planCode: string | null;
  readonly activeSubscription: boolean;
  readonly features: Readonly<Record<string, boolean>> | null;
  readonly limits: Readonly<Record<string, number>> | null;
  readonly resolvedAt: string;
}
