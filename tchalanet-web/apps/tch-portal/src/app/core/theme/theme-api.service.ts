import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { ApiResponse, RuntimeTheme, ThemeMode } from '../../shared/types';
import { unwrapApiResponse } from '../http';

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
    tokens: normalizeTokens(view.tokens),
  };
}

function toThemeMode(value: string): ThemeMode {
  const normalized = value.toLowerCase();
  return normalized === 'dark' || normalized === 'system' ? normalized : 'light';
}

function toEffectiveThemeMode(value: string): 'light' | 'dark' {
  return value.toLowerCase() === 'dark' ? 'dark' : 'light';
}

function normalizeTokens(tokens: Readonly<Record<string, string>>): Readonly<Record<string, string>> {
  return Object.fromEntries(
    Object.entries(tokens).map(([key, value]) => [key.startsWith('--') ? key : `--${key}`, value]),
  );
}
