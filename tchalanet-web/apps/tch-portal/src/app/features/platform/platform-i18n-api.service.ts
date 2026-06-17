import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import { Observable } from 'rxjs';
import { TchPageResult } from './platform-ops-api.service';

export interface I18nOverrideView {
  id: { value: string };
  locale: string;
  key: string;
  value: string;
  createdAt: string;
  updatedAt?: string;
}

export interface I18nGlobalOverviewView {
  generatedAt: string;
  summary: { totalKeys: number; totalLocales: number; totalOverrides: number };
}

@Injectable({ providedIn: 'root' })
export class PlatformI18nApi {
  private readonly backend = inject(TchBackendClient);

  listOverrides(params: { locale?: string; q?: string; page?: number; size?: number }): Observable<TchPageResult<I18nOverrideView>> {
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

  createOverride(body: { locale: string; key: string; value: string }): Observable<I18nOverrideView> {
    return this.backend.post<I18nOverrideView>('/platform/i18n-overrides', body);
  }

  updateOverride(id: string, body: { value: string }): Observable<I18nOverrideView> {
    return this.backend.put<I18nOverrideView>(`/platform/i18n-overrides/${id}`, body);
  }

  deleteOverride(id: string): Observable<void> {
    return this.backend.delete<void>(`/platform/i18n-overrides/${id}`);
  }

  resolveLocale(locale: string): Observable<Record<string, string>> {
    return this.backend.get<Record<string, string>>(`/platform/i18n-overrides/resolve/${locale}`);
  }
}
