import { HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { RuntimeSettingsSource, toRuntimeSettings } from './settings.mapper';

@Injectable({ providedIn: 'root' })
export class SettingsApi {
  private readonly backend = inject(TchBackendClient);

  getPublicSettings(namespace?: string): Observable<RuntimeSettingsSource> {
    const params = namespace ? new HttpParams().set('namespace', namespace) : undefined;
    return this.backend
      .get<readonly SettingsApiSetting[]>('/public/settings', { params })
      .pipe(map(toRuntimeSettings));
  }

  getPrivateSettings(namespaces: readonly string[] = []): Observable<RuntimeSettingsSource> {
    const params = namespaces.reduce(
      (acc, namespace) => acc.append('namespaces', namespace),
      new HttpParams(),
    );
    return this.backend
      .get<readonly SettingsApiSetting[]>('/tenant/settings/resolve', { params })
      .pipe(map(toRuntimeSettings));
  }
}

export interface SettingsApiSetting {
  readonly namespace: string;
  readonly settingKey: string;
  readonly settingValue: string;
  readonly valueType: SettingValueType;
}

type SettingValueType = 'STRING' | 'INT' | 'LONG' | 'DECIMAL' | 'BOOLEAN' | 'JSON';
