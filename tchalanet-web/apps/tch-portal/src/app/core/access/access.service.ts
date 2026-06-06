import { Injectable, inject } from '@angular/core';

import { EntitlementsStore } from '../entitlement';
import { FeatureFlags } from '@tch/shared-config';

/**
 * A single access requirement combining the two orthogonal gating concerns:
 * - `feature`: is the capability built/turned on? (feature flag)
 * - `entitlement`: is this tenant/plan allowed to use it? (exported entitlement)
 * Each part is optional; omit one to gate on the other alone.
 */
export interface AccessRequirement {
  readonly feature?: string;
  readonly featureDefault?: boolean;
  readonly entitlement?: string;
}

/**
 * Combines feature flags and entitlements into one decision so templates and routes don't grow
 * `featureEnabled && hasEntitlement` chains. Reactive: reads the underlying signals.
 */
@Injectable({ providedIn: 'root' })
export class AccessService {
  private readonly features = inject(FeatureFlags);
  private readonly entitlements = inject(EntitlementsStore);

  can(req: AccessRequirement): boolean {
    const featureOk =
      req.feature == null || this.features.isEnabled(req.feature, req.featureDefault ?? false);
    const entitlementOk = req.entitlement == null || this.entitlements.has(req.entitlement);
    return featureOk && entitlementOk;
  }
}
