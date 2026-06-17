import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import { Observable } from 'rxjs';
import { TchPageResult } from './platform-ops-api.service';

export interface SettingView {
  id: { value: string };
  namespace: string;
  settingKey: string;
  settingValue: string;
  valueType: 'STRING' | 'BOOLEAN' | 'INTEGER' | 'DECIMAL' | 'JSON';
  level: 'GLOBAL' | 'TENANT' | 'OUTLET' | 'TERMINAL';
  exposure: 'PUBLIC_RUNTIME' | 'TENANT_RUNTIME' | 'ADMIN_RUNTIME' | 'INTERNAL';
  active: boolean;
}

export interface SettingsCatalogStatsView {
  totalKeys: number;
  totalLocales?: number;
  totalOverrides?: number;
}

@Injectable({ providedIn: 'root' })
export class PlatformSettingsApi {
  private readonly backend = inject(TchBackendClient);

  listSettings(params: { namespace?: string; q?: string; page?: number; size?: number }): Observable<TchPageResult<SettingView>> {
    const q = new URLSearchParams(
      Object.fromEntries(
        Object.entries(params)
          .filter(([, v]) => v !== undefined && v !== '')
          .map(([k, v]) => [k, String(v)]),
      ),
    ).toString();
    return this.backend.get<TchPageResult<SettingView>>(`/platform/settings${q ? '?' + q : ''}`);
  }

  getOverview(): Observable<SettingsCatalogStatsView> {
    return this.backend.get<SettingsCatalogStatsView>('/platform/settings/overview');
  }

  getSetting(id: string): Observable<SettingView> {
    return this.backend.get<SettingView>(`/platform/settings/${id}`);
  }

  createSetting(body: Partial<SettingView>): Observable<SettingView> {
    return this.backend.post<SettingView>('/platform/settings', body);
  }

  updateSetting(id: string, body: Partial<SettingView>): Observable<SettingView> {
    return this.backend.put<SettingView>(`/platform/settings/${id}`, body);
  }

  deleteSetting(id: string): Observable<void> {
    return this.backend.delete<void>(`/platform/settings/${id}`);
  }
}
