export interface TenantAdminGlobalRow {
  readonly id: string;
  readonly displayName: string | null;
  readonly email: string | null;
  readonly status: string;
  readonly tenantId: string | null;
  readonly tenantName: string | null;
  readonly tenantCode: string | null;
  readonly createdAt: string | null;
}

export interface TenantAdminGlobalPage {
  readonly items: TenantAdminGlobalRow[];
  readonly totalElements: number;
  readonly page: number;
  readonly size: number;
  readonly totalPages: number;
  readonly hasNext: boolean;
  readonly hasPrevious: boolean;
}
