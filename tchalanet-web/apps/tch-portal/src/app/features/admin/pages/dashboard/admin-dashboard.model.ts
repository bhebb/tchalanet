export type DashboardUiState = 'loading' | 'ready' | 'partial' | 'error';

export interface AdminDashboardView {
  readonly kpis: AdminDashboardKpis;
  readonly onboarding: TenantOnboardingSummary;
  readonly recentSales: readonly TenantRecentSaleItem[];
  readonly attentionItems: readonly TenantAttentionItem[];
  readonly notices?: readonly DashboardNotice[];
}

export interface AdminDashboardKpis {
  readonly sellers: number | null;
  readonly outlets: number | null;
  readonly terminals: number | null;
  readonly openSessions: number | null;
}

export interface TenantOnboardingSummary {
  readonly status: 'NOT_STARTED' | 'IN_PROGRESS' | 'READY' | 'BLOCKED';
  readonly completedSteps: number;
  readonly totalSteps: number;
}

export interface TenantRecentSaleItem {
  readonly reference: string;
  readonly amount: number;
  readonly currency: string;
  readonly createdAtLabel: string;
}

export interface TenantAttentionItem {
  readonly id: string;
  readonly label: string;
  readonly severity: 'INFO' | 'WARN' | 'ERROR';
}

export interface DashboardNotice {
  readonly code: string;
  readonly severity: 'INFO' | 'WARN' | 'ERROR';
  readonly message: string;
}
