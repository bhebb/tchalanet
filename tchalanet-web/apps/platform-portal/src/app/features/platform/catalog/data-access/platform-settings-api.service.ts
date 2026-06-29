import { Injectable, inject } from '@angular/core';
import { TchBackendClient, TchPage as TchPageResult } from '@tch/api';
import { Observable } from 'rxjs';

export type SettingValueType = 'STRING' | 'INT' | 'LONG' | 'DECIMAL' | 'BOOLEAN' | 'JSON';
export type SettingLevel = 'GLOBAL' | 'TENANT' | 'OUTLET' | 'TERMINAL';
export type SettingExposure = 'INTERNAL' | 'PUBLIC_RUNTIME' | 'TENANT_RUNTIME' | 'ADMIN_RUNTIME';

export interface SettingView {
  id: string;
  namespace: string;
  settingKey: string;
  settingValue: string;
  valueType: SettingValueType;
  level: SettingLevel;
  exposure: SettingExposure;
  tenantId?: { value: string } | null;
  outletId?: { value: string } | null;
  terminalId?: { value: string } | null;
  active: boolean;
}

export interface SettingsCatalogStatsView {
  totalGlobalSettings: number;
  totalTenantSettings: number;
  totalActiveSettings: number;
}

/** POST /platform/settings */
export interface CreateSettingRequest {
  namespace: string;
  settingKey: string;
  settingValue: string;
  valueType: SettingValueType;
  level: SettingLevel;
  exposure?: SettingExposure;
  tenantId?: string;
}

/** PUT /platform/settings/{id} — patch semantics, null = no change */
export interface UpdateSettingRequest {
  settingValue?: string;
  exposure?: SettingExposure;
  active?: boolean;
}

@Injectable({ providedIn: 'root' })
export class PlatformSettingsApi {
  private readonly backend = inject(TchBackendClient);

  listSettings(params: {
    namespace?: string;
    settingKey?: string;
    level?: SettingLevel;
    exposure?: SettingExposure;
    active?: boolean;
    page?: number;
    size?: number;
  }): Observable<TchPageResult<SettingView>> {
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

  createSetting(body: CreateSettingRequest): Observable<SettingView> {
    return this.backend.post<SettingView>('/platform/settings', body);
  }

  updateSetting(id: string, body: UpdateSettingRequest): Observable<SettingView> {
    return this.backend.put<SettingView>(`/platform/settings/${id}`, body);
  }

  deleteSetting(id: string): Observable<void> {
    return this.backend.delete<void>(`/platform/settings/${id}`);
  }
}
