import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';

import { AccessRequirement, AccessService } from './access.service';

export interface AccessGuardOptions {
  /** Where to send the user when access is denied. Defaults to `/forbidden`. */
  readonly redirectTo?: string;
}

/**
 * Route guard for a combined feature + entitlement requirement. Redirects on denial.
 * Note: flags/entitlements load asynchronously; a guard evaluated before they resolve uses the
 * conservative defaults (feature off, entitlement absent).
 */
export function accessGuard(req: AccessRequirement, options?: AccessGuardOptions): CanActivateFn {
  return (): boolean | UrlTree => {
    const access = inject(AccessService);
    const router = inject(Router);

    return access.can(req) ? true : router.parseUrl(options?.redirectTo ?? '/forbidden');
  };
}
