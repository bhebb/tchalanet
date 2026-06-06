import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { ApiResponse, unwrapApiResponse } from '@tch/api';
import { map, Observable } from 'rxjs';

import { RuntimeSettingsSource, toRuntimeSettings } from './settings.mapper';

@Injectable({ providedIn: 'root' })
export class SettingsApi {
  private readonly http = inject(HttpClient);

  getPublicSettings(namespace?: string): Observable<RuntimeSettingsSource> {
    const params = namespace ? new HttpParams().set('namespace', namespace) : undefined;

    return this.http
      .get<ApiResponse<readonly SettingsApiSetting[]>>('/api/v1/public/settings', { params })
      .pipe(map(response => toRuntimeSettings(unwrapApiResponse(response))));
  }

  getPrivateSettings(namespaces: readonly string[] = []): Observable<RuntimeSettingsSource> {
    const params = namespaces.reduce(
      (acc, namespace) => acc.append('namespaces', namespace),
      new HttpParams(),
    );

    return this.http
      .get<ApiResponse<readonly SettingsApiSetting[]>>('/api/v1/tenant/settings/resolve', {
        params,
      })
      .pipe(map(response => toRuntimeSettings(unwrapApiResponse(response))));
  }
}

export interface SettingsApiSetting {
  readonly namespace: string;
  readonly settingKey: string;
  readonly settingValue: string;
  readonly valueType: SettingValueType;
}

type SettingValueType = 'STRING' | 'INT' | 'LONG' | 'DECIMAL' | 'BOOLEAN' | 'JSON';
