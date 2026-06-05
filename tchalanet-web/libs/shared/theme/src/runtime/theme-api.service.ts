import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { mapBackendThemeTokens } from './theme-token-map';
import { RuntimeTheme, ThemeMode } from './theme.types';

interface ApiResponse<T> {
  readonly data: T;
}

interface ThemeRuntimeApiView {
  readonly presetCode: string;
  readonly mode: string;
  readonly tokens: Readonly<Record<string, string>>;
  readonly isDefault: boolean;
  readonly version: number;
}

@Injectable({ providedIn: 'root' })
export class ThemeApi {
  private readonly http = inject(HttpClient);

  getPublicTheme(mode?: ThemeMode): Observable<RuntimeTheme> {
    const params = mode && mode !== 'system' ? new HttpParams().set('mode', mode) : undefined;

    return this.http
      .get<ApiResponse<ThemeRuntimeApiView>>('/api/v1/public/theme/runtime', { params })
      .pipe(map((response) => toRuntimeTheme(unwrapApiResponse(response))));
  }

  getPrivateTheme(mode?: ThemeMode): Observable<RuntimeTheme> {
    const params = mode && mode !== 'system' ? new HttpParams().set('mode', mode) : undefined;

    return this.http
      .get<ApiResponse<ThemeRuntimeApiView>>('/api/v1/tenant/theme/runtime', { params })
      .pipe(map((response) => toRuntimeTheme(unwrapApiResponse(response))));
  }
}

function toRuntimeTheme(view: ThemeRuntimeApiView): RuntimeTheme {
  return {
    activePresetKey: view.presetCode || 'tchalanet',
    mode: toThemeMode(view.mode),
    effectiveMode: toEffectiveThemeMode(view.mode),
    tokens: mapBackendThemeTokens(view.tokens),
  };
}

function unwrapApiResponse<T>(response: ApiResponse<T>): T {
  return response.data;
}

function toThemeMode(value: string): ThemeMode {
  const normalized = value.toLowerCase();
  return normalized === 'dark' || normalized === 'system' ? normalized : 'light';
}

function toEffectiveThemeMode(value: string): 'light' | 'dark' {
  return value.toLowerCase() === 'dark' ? 'dark' : 'light';
}
