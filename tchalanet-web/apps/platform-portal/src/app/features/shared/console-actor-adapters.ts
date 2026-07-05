import { ConsoleActorIdentity, consoleSellerTerminalActorIdentity } from '@tch/web/console';

export interface PlatformSuperAdminActorSource {
  readonly id: string;
  readonly email?: string | null;
  readonly displayName?: string | null;
  readonly status: string;
  readonly assignedAt?: string | null;
}

export interface PlatformTenantAdminActorSource {
  readonly id: string;
  readonly email?: string | null;
  readonly phone?: string | null;
  readonly displayName?: string | null;
  readonly status: string;
  readonly roleCodes?: readonly string[] | null;
  readonly createdAt?: string | null;
  readonly tenantId?: string | null;
  readonly tenantName?: string | null;
  readonly tenantCode?: string | null;
}

export interface PlatformSellerTerminalActorSource {
  readonly id: { readonly value: string } | string;
  readonly terminalCode?: string | null;
  readonly displayName?: string | null;
  readonly email?: string | null;
  readonly phoneNumber?: string | null;
  readonly status: string;
  readonly tenantId?: string | null;
  readonly tenantName?: string | null;
  readonly tenantCode?: string | null;
}

export function platformSuperAdminActorIdentity(
  admin: PlatformSuperAdminActorSource,
): ConsoleActorIdentity {
  return {
    kind: 'SUPER_ADMIN',
    id: admin.id,
    email: admin.email,
    displayName: admin.displayName,
    status: admin.status,
    assignedAt: admin.assignedAt,
  };
}

export function platformTenantAdminActorIdentity(
  admin: PlatformTenantAdminActorSource,
): ConsoleActorIdentity {
  return {
    kind: 'TENANT_ADMIN',
    id: admin.id,
    email: admin.email,
    phone: admin.phone,
    displayName: admin.displayName,
    status: admin.status,
    roleCodes: admin.roleCodes,
    createdAt: admin.createdAt,
    tenantId: admin.tenantId,
    tenantName: admin.tenantName,
    tenantCode: admin.tenantCode,
  };
}

export function platformSellerTerminalActorIdentity(
  terminal: PlatformSellerTerminalActorSource,
): ConsoleActorIdentity {
  return consoleSellerTerminalActorIdentity(terminal);
}
