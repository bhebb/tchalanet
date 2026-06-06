import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';

import { FeatureFlags } from './feature-flags';

export interface FeatureGuardOptions {
  /** Value used when the flag has not been resolved yet. Defaults to `false`. */
  readonly default?: boolean;
  /** Where to send the user when the feature is disabled. Defaults to `/forbidden`. */
  readonly redirectTo?: string;
}

/**
 * Route guard that blocks a route behind a runtime feature flag.
 *
 * Resolves through the `FeatureFlags` seam; if the flag is off, redirects to `redirectTo`.
 * Note: settings load asynchronously during bootstrap, so a guard evaluated before settings
 * resolve falls back to `options.default`. For routes that must hard-gate a not-ready feature,
 * prefer a conservative default (`false`). For combined feature + entitlement gating use
 * `accessGuard`.
 */
export function featureGuard(key: string, options?: FeatureGuardOptions): CanActivateFn {
  return (): boolean | UrlTree => {
    const features = inject(FeatureFlags);
    const router = inject(Router);

    if (features.isEnabled(key, options?.default ?? false)) {
      return true;
    }

    return router.parseUrl(options?.redirectTo ?? '/forbidden');
  };
}
