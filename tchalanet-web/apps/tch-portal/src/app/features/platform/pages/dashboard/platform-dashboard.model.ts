export type DashboardUiState = 'loading' | 'ready' | 'partial' | 'error';

export interface PlatformDashboardView {
  readonly kpis: PlatformDashboardKpis;
  readonly provisioning: readonly PlatformProvisioningProgressItem[];
  readonly operationalIntegrity: PlatformOperationalIntegrityView | null;
  readonly recentContactRequests: readonly PlatformRecentContactRequestItem[];
  readonly notices?: readonly PlatformDashboardNotice[];
}

export interface PlatformDashboardKpis {
  readonly activeTenants: number | null;
  readonly activeTenantsTrendPercent?: number | null;
  readonly receivedContacts: number | null;
  readonly pendingNotifications: number | null;
  readonly degradedServices: number | null;
}

export interface PlatformProvisioningProgressItem {
  readonly tenantName: string;
  readonly tenantCode: string;
  readonly progressPercent: number;
  readonly status: 'PENDING' | 'PROVISIONING' | 'HEALTHY' | 'BLOCKED' | 'FAILED';
}

export interface PlatformOperationalIntegrityView {
  readonly scorePercent: number | null;
  readonly lastAuditLabel: string | null;
  readonly status: 'READY' | 'WARNING' | 'DEGRADED' | 'MISSING';
  readonly message: string | null;
}

export interface PlatformRecentContactRequestItem {
  readonly reference: string;
  readonly requesterName: string;
  readonly requesterLabel: string | null;
  readonly subject: string;
  readonly createdAtLabel: string;
  readonly status: 'RECEIVED' | 'PENDING' | 'PROCESSING' | 'CLOSED';
}

export interface PlatformDashboardNotice {
  readonly code: string;
  readonly severity: 'INFO' | 'WARN' | 'ERROR';
  readonly message: string;
}
