import { DOCUMENT, isPlatformBrowser } from '@angular/common';
import { Injectable, PLATFORM_ID, computed, inject, signal } from '@angular/core';

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
  private static readonly STORAGE_KEY = 'tch.support.tenantAdminAccess';

  private readonly document = inject(DOCUMENT);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly _session = signal<TenantAdminAccessSession | null>(this.readStoredSession());

  readonly session = this._session.asReadonly();

  readonly isActive = computed(() => this._session() !== null);

  readonly mode = computed(() => this._session()?.mode ?? null);

  readonly tenantName = computed(() => this._session()?.tenantName ?? null);

  startSession(session: TenantAdminAccessSession): void {
    this._session.set(session);
    this.storage()?.setItem(SupportAccessStore.STORAGE_KEY, JSON.stringify(session));
  }

  clearSession(): void {
    this._session.set(null);
    this.storage()?.removeItem(SupportAccessStore.STORAGE_KEY);
  }

  private readStoredSession(): TenantAdminAccessSession | null {
    const raw = this.storage()?.getItem(SupportAccessStore.STORAGE_KEY);
    if (!raw) return null;

    try {
      return JSON.parse(raw) as TenantAdminAccessSession;
    } catch {
      this.storage()?.removeItem(SupportAccessStore.STORAGE_KEY);
      return null;
    }
  }

  private storage(): Storage | null {
    if (!isPlatformBrowser(this.platformId)) {
      return null;
    }
    return this.document.defaultView?.sessionStorage ?? null;
  }
}
