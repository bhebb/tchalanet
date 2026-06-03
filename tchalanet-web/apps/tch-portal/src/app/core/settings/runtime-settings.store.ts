import { computed, inject, Injectable, signal } from '@angular/core';
import { catchError, of, tap } from 'rxjs';

import { RuntimeSettings } from '../../shared/types';
import { SettingsApi } from './settings-api.service';
import { defaultRuntimeSettings } from './settings.mapper';

type SettingsLoadState = 'idle' | 'loading' | 'ready' | 'fallback';

@Injectable({ providedIn: 'root' })
export class RuntimeSettingsStore {
  private readonly api = inject(SettingsApi);
  private readonly settingsState = signal<RuntimeSettings>(defaultRuntimeSettings);
  private readonly loadStateState = signal<SettingsLoadState>('idle');

  readonly settings = this.settingsState.asReadonly();
  readonly loadState = this.loadStateState.asReadonly();
  readonly ready = computed(() => this.loadState() === 'ready' || this.loadState() === 'fallback');

  loadPublicSettings(): void {
    this.loadStateState.set('loading');

    this.api
      .getPublicSettings()
      .pipe(
        catchError(() => of(null)),
        tap(settings => this.applyLoadedSettings(settings)),
      )
      .subscribe();
  }

  loadPrivateSettings(namespaces: readonly string[] = []): void {
    this.loadStateState.set('loading');

    this.api
      .getPrivateSettings(namespaces)
      .pipe(
        catchError(() => of(null)),
        tap(settings => this.applyLoadedSettings(settings)),
      )
      .subscribe();
  }

  isFeatureEnabled(key: string, defaultValue = false): boolean {
    return this.settings().featureFlags[key]?.enabled ?? defaultValue;
  }

  private applyLoadedSettings(settings: RuntimeSettings | null): void {
    if (!settings) {
      this.settingsState.set(defaultRuntimeSettings);
      this.loadStateState.set('fallback');
      return;
    }

    this.settingsState.set(settings);
    this.loadStateState.set('ready');
  }
}
