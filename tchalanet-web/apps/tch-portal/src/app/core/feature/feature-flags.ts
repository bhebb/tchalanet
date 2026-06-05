import { Injectable, inject } from '@angular/core';

import { RuntimeSettingsStore } from '../settings';

/**
 * Provider-agnostic feature-flag access — the isolation seam for feature management.
 *
 * Call sites (directives, guards, services) depend on this abstract token, never on the settings
 * store directly. Today flags are a constrained subset of runtime settings (`feature.*`
 * namespace); if a dedicated provider such as Unleash is introduced later, only the bound
 * implementation changes — no call site moves.
 */
export abstract class FeatureFlags {
  abstract isEnabled(key: string, defaultValue?: boolean): boolean;
}

/** Default implementation: reads flags from the `feature.*` runtime settings namespace. */
@Injectable({ providedIn: 'root' })
export class SettingsFeatureFlags extends FeatureFlags {
  private readonly settings = inject(RuntimeSettingsStore);

  isEnabled(key: string, defaultValue = false): boolean {
    return this.settings.isFeatureEnabled(key, defaultValue);
  }
}
