import { InjectionToken } from '@angular/core';

/**
 * Base path for all Tchalanet API calls.
 *
 * Default: `/api/v1`
 *
 * Override at bootstrap (e.g. to switch to v2) with:
 * ```ts
 * { provide: TCH_API_BASE, useValue: '/api/v2' }
 * ```
 * or for environment-driven config:
 * ```ts
 * { provide: TCH_API_BASE, useFactory: () => environment.apiBase }
 * ```
 */
export const TCH_API_BASE = new InjectionToken<string>('TCH_API_BASE', {
  factory: () => '/api/v1',
});
