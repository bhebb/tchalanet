import { DOCUMENT, isPlatformBrowser } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { DestroyRef, Injectable, PLATFORM_ID, computed, inject, signal } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import { firstValueFrom } from 'rxjs';

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

  private readonly backend = inject(TchBackendClient);
  private readonly destroyRef = inject(DestroyRef);
  private readonly document = inject(DOCUMENT);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly _session = signal<TenantAdminAccessSession | null>(this.readStoredSession());

  readonly session = this._session.asReadonly();

  readonly isActive = computed(() => this._session() !== null);

  readonly mode = computed(() => this._session()?.mode ?? null);

  readonly tenantName = computed(() => this._session()?.tenantName ?? null);

  constructor() {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    const window = this.document.defaultView;
    if (!window) {
      return;
    }
    const hydrate = () => void this.hydrateCurrent();
    window.addEventListener('focus', hydrate);
    window.document.addEventListener('visibilitychange', hydrate);
    this.destroyRef.onDestroy(() => {
      window.removeEventListener('focus', hydrate);
      window.document.removeEventListener('visibilitychange', hydrate);
    });
    void this.hydrateCurrent();
  }

  startSession(session: TenantAdminAccessSession): void {
    this._session.set(session);
    this.storage()?.setItem(SupportAccessStore.STORAGE_KEY, JSON.stringify(session));
  }

  async hydrateCurrent(): Promise<void> {
    try {
      const session = await firstValueFrom(
        this.backend.get<TenantAdminAccessSession | null>('/platform/tenants/admin-access/current', {
          suppressShellFeedback: true,
        }),
      );
      if (!session) {
        this.clearSession();
        return;
      }
      this.startSession(session);
    } catch (err) {
      if (isAbsentSupportSession(err)) {
        this.clearSession();
      }
    }
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

function isAbsentSupportSession(err: unknown): boolean {
  return err instanceof HttpErrorResponse && (err.status === 401 || err.status === 403 || err.status === 404 || err.status === 410);
}
