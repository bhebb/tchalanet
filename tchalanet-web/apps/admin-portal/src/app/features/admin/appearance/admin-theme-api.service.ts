import { Injectable, inject } from '@angular/core';
import { TchBackendClient, TchRequestOptions } from '@tch/api';
import { Observable } from 'rxjs';

/** Admin view of the tenant theme (GET /admin/theme). */
export interface TenantThemeAdminView {
  presetCode: string;
  defaultMode: string;
  active: boolean;
  isDefault: boolean;
  version: number;
  updatedAt: string;
}

/** Available preset (GET /admin/theme/presets). */
export interface ThemePresetView {
  code: string;
  vendor: string;
  labelKey: string;
  config?: ThemePresetConfig | null;
  active: boolean;
  isDefault: boolean;
}

export interface ThemePresetConfig {
  defaultMode?: string;
  tokens?: {
    light?: Record<string, string>;
    dark?: Record<string, string>;
  };
}

@Injectable({ providedIn: 'root' })
export class AdminThemeApi {
  private readonly backend = inject(TchBackendClient);

  get(options?: TchRequestOptions): Observable<TenantThemeAdminView | null> {
    return this.backend.get<TenantThemeAdminView | null>('/admin/theme', options);
  }

  listPresets(options?: TchRequestOptions): Observable<readonly ThemePresetView[]> {
    return this.backend.get<readonly ThemePresetView[]>('/admin/theme/presets', options);
  }

  /** Requires `theme.manage` permission AND the `theme.preset_selection` plan feature. */
  applyPreset(presetCode: string, options?: TchRequestOptions): Observable<void> {
    return this.backend.post<void>('/admin/theme/preset', { presetCode }, options);
  }

  updateMode(defaultMode: string, options?: TchRequestOptions): Observable<void> {
    return this.backend.patch<void>('/admin/theme/settings', { defaultMode }, options);
  }

  reset(options?: TchRequestOptions): Observable<void> {
    return this.backend.delete<void>('/admin/theme', options);
  }
}
