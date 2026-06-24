import { Injectable, computed, signal } from '@angular/core';

export interface TenantAdminAccessSession {
  sessionId: string;
  tenantId: string;
  tenantCode: string;
  tenantName: string;
  startedAt: string;
  expiresAt?: string | null;
  actorRole: 'SUPER_ADMIN';
  mode: 'SUPPORT_OVERRIDE' | 'SUPPORT_READONLY';
  sensitiveDataMasked: boolean;
}

@Injectable({ providedIn: 'root' })
export class SupportAccessStore {
  private readonly _session = signal<TenantAdminAccessSession | null>(null);

  readonly session = this._session.asReadonly();

  readonly isActive = computed(() => this._session() !== null);

  readonly mode = computed(() => this._session()?.mode ?? null);

  readonly tenantName = computed(() => this._session()?.tenantName ?? null);

  startSession(session: TenantAdminAccessSession): void {
    this._session.set(session);
  }

  clearSession(): void {
    this._session.set(null);
  }
}
