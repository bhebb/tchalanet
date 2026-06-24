import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import { Observable } from 'rxjs';
import { TchPage as TchPageResult } from './platform-ops-api.service';

export type I18nOverrideLevel = 'GLOBAL' | 'TENANT';
export type I18nSurface =
  | 'PUBLIC_HOME' | 'PUBLIC_RESULTS' | 'PUBLIC_TICKET_CHECK' | 'COMMON_PUBLIC_ERROR'
  | 'AUTH' | 'CASHIER' | 'TENANT_ADMIN' | 'PLATFORM_ADMIN' | 'COMMON_PRIVATE_ERROR' | 'INTERNAL';

export interface I18nOverrideView {
  id: string;
  level: I18nOverrideLevel;
  tenantId?: string | null;
  surface: I18nSurface;
  locale: string;
  i18nKey: string;
  i18nValue: string;
  active: boolean;
  createdAt: string;
  updatedAt?: string;
}

export interface I18nGlobalOverviewView {
  generatedAt: string;
  summary: { totalKeys: number; totalLocales: number; totalOverrides: number };
}

/** POST /platform/i18n-overrides */
export interface CreateI18nOverrideRequest {
  locale: string;
  level: I18nOverrideLevel;
  surface?: I18nSurface;
  i18nKey: string;
  i18nValue: string;
  tenantId?: string;
}

/** PUT /platform/i18n-overrides/{id} — patch semantics */
export interface UpdateI18nOverrideRequest {
  level?: I18nOverrideLevel;
  surface?: I18nSurface;
  i18nValue?: string;
  active?: boolean;
}

@Injectable({ providedIn: 'root' })
export class PlatformI18nApi {
  private readonly backend = inject(TchBackendClient);

  listOverrides(params: {
    locale?: string;
    level?: I18nOverrideLevel;
    i18nKeyContains?: string;
    active?: boolean;
    page?: number;
    size?: number;
  }): Observable<TchPageResult<I18nOverrideView>> {
    const q = new URLSearchParams(
      Object.fromEntries(
        Object.entries(params)
          .filter(([, v]) => v !== undefined && v !== '')
          .map(([k, v]) => [k, String(v)]),
      ),
    ).toString();
    return this.backend.get<TchPageResult<I18nOverrideView>>(`/platform/i18n-overrides${q ? '?' + q : ''}`);
  }

  getOverview(): Observable<I18nGlobalOverviewView> {
    return this.backend.get<I18nGlobalOverviewView>('/platform/i18n-overrides/overview');
  }

  createOverride(body: CreateI18nOverrideRequest): Observable<I18nOverrideView> {
    return this.backend.post<I18nOverrideView>('/platform/i18n-overrides', body);
  }

  updateOverride(id: string, body: UpdateI18nOverrideRequest): Observable<I18nOverrideView> {
    return this.backend.put<I18nOverrideView>(`/platform/i18n-overrides/${id}`, body);
  }

  deleteOverride(id: string): Observable<void> {
    return this.backend.delete<void>(`/platform/i18n-overrides/${id}`);
  }

  resolveLocale(locale: string, tenantId?: string): Observable<Record<string, string>> {
    const q = tenantId ? `?locale=${locale}&tenantId=${tenantId}` : `?locale=${locale}`;
    return this.backend.get<Record<string, string>>(`/platform/i18n-overrides/resolve${q}`);
  }
}
