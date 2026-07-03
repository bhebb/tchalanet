import { Injectable, inject } from '@angular/core';

import { AuthSessionService } from '../auth-session.service';
import { EntitlementsStore } from '../entitlement';
import { UserRole } from '../auth.types';
import { FeatureFlags } from '@tch/shared-config';

/**
 * A single access requirement combining the two orthogonal gating concerns:
 * - `feature`: is the capability built/turned on? (feature flag)
 * - `entitlement`: is this tenant/plan allowed to use it? (exported entitlement)
 * - `role` / `permission`: is the current actor allowed to use it? (runtime access)
 * Each part is optional; omit one to gate on the other alone.
 * An array of requirements means OR.
 */
export interface AccessRequirement {
  readonly feature?: string;
  readonly featureDefault?: boolean;
  readonly entitlement?: string;
  readonly role?: UserRole | readonly UserRole[];
  readonly permission?: string | readonly string[];
}

/**
 * Combines feature flags, entitlements and runtime access into one decision so templates and routes
 * don't grow `featureEnabled && hasEntitlement && hasPermission` chains.
 * Reactive: reads the underlying signals.
 */
@Injectable({ providedIn: 'root' })
export class AccessService {
  private readonly features = inject(FeatureFlags);
  private readonly entitlements = inject(EntitlementsStore);
  private readonly auth = inject(AuthSessionService);

  can(req: AccessRequirement | readonly AccessRequirement[]): boolean {
    if (isReadonlyArray(req)) {
      return req.some(item => this.canOne(item));
    }
    return this.canOne(req);
  }

  private canOne(req: AccessRequirement): boolean {
    const featureOk =
      req.feature == null || this.features.isEnabled(req.feature, req.featureDefault ?? false);
    const entitlementOk = req.entitlement == null || this.entitlements.has(req.entitlement);
    const roleOk = req.role == null || this.any(req.role, role => this.auth.hasRole(role));
    const permissionOk =
      req.permission == null || this.any(req.permission, permission => this.auth.hasPermission(permission));
    return featureOk && entitlementOk && roleOk && permissionOk;
  }

  private any<T>(value: T | readonly T[], predicate: (item: T) => boolean): boolean {
    return isReadonlyArray(value) ? value.some(predicate) : predicate(value);
  }
}

function isReadonlyArray<T>(value: T | readonly T[]): value is readonly T[] {
  return Array.isArray(value);
}
