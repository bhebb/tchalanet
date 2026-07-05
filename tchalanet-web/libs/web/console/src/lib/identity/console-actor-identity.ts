import type { BadgeStatus } from '@tch/ui/components';

export type ConsoleActorKind =
  | 'SUPER_ADMIN'
  | 'TENANT_ADMIN'
  | 'SELLER_TERMINAL'
  | 'SELLER'
  | 'SUPPORT_OPERATOR';

export interface ConsoleActorIdentity {
  readonly kind: ConsoleActorKind;
  readonly id: string;
  readonly displayName?: string | null;
  readonly email?: string | null;
  readonly phone?: string | null;
  readonly status: string;
  readonly roleCodes?: readonly string[] | null;
  readonly assignedAt?: string | null;
  readonly createdAt?: string | null;
  readonly tenantId?: string | null;
  readonly tenantName?: string | null;
  readonly tenantCode?: string | null;
  readonly code?: string | null;
}

export interface ConsoleSellerTerminalActorSource {
  readonly id?: { readonly value: string } | string | null;
  readonly sellerTerminalId?: string | null;
  readonly terminalCode?: string | null;
  readonly displayName?: string | null;
  readonly email?: string | null;
  readonly phoneNumber?: string | null;
  readonly phone?: string | null;
  readonly status: string;
  readonly tenantId?: { readonly value: string } | string | null;
  readonly tenantName?: string | null;
  readonly tenantCode?: string | null;
}

export function consoleActorPrimaryLabel(actor: ConsoleActorIdentity): string {
  return firstText(actor.displayName, actor.email, actor.code, actor.tenantCode, actor.id) ?? '-';
}

export function consoleActorSecondaryLabel(actor: ConsoleActorIdentity): string | null {
  if (actor.kind === 'SELLER_TERMINAL') {
    return joinLabels(actor.code, actor.email, actor.phone);
  }
  return firstText(actor.email, actor.phone, actor.tenantName, actor.tenantCode);
}

export function consoleActorTenantLabel(actor: ConsoleActorIdentity): string | null {
  return firstText(actor.tenantName, actor.tenantCode, actor.tenantId);
}

export function consoleActorStatusBadge(status: string | null | undefined): BadgeStatus {
  const normalized = status?.trim().toUpperCase();
  const map: Record<string, BadgeStatus> = {
    ACTIVE: 'ready',
    BLOCKED: 'blocked',
    DISABLED: 'blocked',
    PENDING: 'pending',
    SUSPENDED: 'warning',
    INACTIVE: 'missing',
    ARCHIVED: 'missing',
    DELETED: 'missing',
  };
  return normalized ? map[normalized] ?? 'missing' : 'missing';
}

export function consoleSellerTerminalActorIdentity(
  terminal: ConsoleSellerTerminalActorSource,
): ConsoleActorIdentity {
  return {
    kind: 'SELLER_TERMINAL',
    id: idValue(terminal.id) ?? terminal.sellerTerminalId ?? terminal.terminalCode ?? '-',
    code: terminal.terminalCode,
    displayName: terminal.displayName,
    email: terminal.email,
    phone: terminal.phoneNumber ?? terminal.phone,
    status: terminal.status,
    tenantId: idValue(terminal.tenantId),
    tenantName: terminal.tenantName,
    tenantCode: terminal.tenantCode,
  };
}

function firstText(...values: readonly (string | null | undefined)[]): string | null {
  for (const value of values) {
    const text = value?.trim();
    if (text) return text;
  }
  return null;
}

function joinLabels(...values: readonly (string | null | undefined)[]): string | null {
  const parts = values
    .map(value => value?.trim())
    .filter((value): value is string => !!value);
  return parts.length ? parts.join(' · ') : null;
}

function idValue(value: { readonly value: string } | string | null | undefined): string | null {
  if (!value) return null;
  return typeof value === 'string' ? value : value.value;
}
