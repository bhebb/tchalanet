export type UserRole = 'CASHIER' | 'TENANT_OWNER' | 'TENANT_ADMIN' | 'SUPER_ADMIN';

export interface UserSession {
  readonly authenticated: boolean;
  readonly userId?: string;
  readonly username?: string;
  readonly displayName?: string;
  readonly tenantId?: string;
  readonly tenantCode?: string;
  readonly roles: readonly UserRole[];
  readonly permissions?: readonly string[];
  readonly tokenExpiresAt?: string;
  readonly entryRoute?: string;
  readonly mustChangePassword?: boolean;
  readonly mustCompleteProfile?: boolean;
}
