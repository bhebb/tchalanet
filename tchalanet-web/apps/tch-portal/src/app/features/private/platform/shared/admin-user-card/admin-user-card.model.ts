export interface AdminUserCardData {
  readonly id: string;
  readonly email: string | null;
  readonly displayName: string | null;
  readonly status: string;
  readonly assignedAt: string | null;
  // enrichment — populated later
  readonly tenantId?: string | null;
  readonly tenantName?: string | null;
  readonly tenantCode?: string | null;
}
