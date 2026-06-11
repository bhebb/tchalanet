import { computed, Injectable, signal } from '@angular/core';

import { FeatureFlag, RuntimeSettings } from '../contracts/runtime.types';
import { defaultRuntimeSettings } from './settings.mapper';

/** Settings carried inside the runtime bootstrap response (no separate settings HTTP call). */
export interface BootstrapSettingsInput {
  readonly locale: string;
  readonly timezone: string;
  readonly currency: string;
  readonly features: Readonly<Record<string, boolean>>;
}

type SettingsLoadState = 'idle' | 'loading' | 'ready' | 'fallback';

@Injectable({ providedIn: 'root' })
export class RuntimeSettingsStore {
  private readonly settingsState = signal<RuntimeSettings>(defaultRuntimeSettings);
  private readonly loadStateState = signal<SettingsLoadState>('idle');

  readonly settings = this.settingsState.asReadonly();
  readonly loadState = this.loadStateState.asReadonly();
  readonly ready = computed(() => this.loadState() === 'ready' || this.loadState() === 'fallback');

  /** Apply settings delivered inside the runtime bootstrap — no extra settings HTTP call. */
  applyBootstrapSettings(input: BootstrapSettingsInput): void {
    const featureFlags: Record<string, FeatureFlag> = {};
    const values: Record<string, unknown> = {
      'app.locale': input.locale,
      'app.timezone': input.timezone,
      'app.currency': input.currency,
    };
    for (const [key, enabled] of Object.entries(input.features ?? {})) {
      featureFlags[key] = { key, enabled };
      values[key] = enabled;
    }
    this.settingsState.set({ featureFlags, values, loadedAt: new Date().toISOString() });
    this.loadStateState.set('ready');
  }

  isFeatureEnabled(key: string, defaultValue = false): boolean {
    return this.settings().featureFlags[key]?.enabled ?? defaultValue;
  }
}
