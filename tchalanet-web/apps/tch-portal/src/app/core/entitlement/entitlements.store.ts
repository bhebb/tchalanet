import { Injectable, computed, inject } from '@angular/core';

import { RuntimeSettingsStore } from '../settings';

const ENTITLEMENT_PREFIX = 'entitlement.';

/**
 * Client-side view of the tenant entitlements we choose to export.
 *
 * Entitlement enforcement is authoritative on the backend (plan limits / quotas); this store only
 * covers UI affordances — e.g. whether to show a link or menu item. It exposes the exported subset,
 * not the full entitlement model.
 *
 * Source today: the `entitlement.*` runtime settings namespace (boolean values), loaded by the
 * private settings bootstrap. It can be repointed to a dedicated endpoint later without changing
 * call sites.
 */
@Injectable({ providedIn: 'root' })
export class EntitlementsStore {
  private readonly settings = inject(RuntimeSettingsStore);

  /** Set of granted entitlement keys (prefix-stripped), reactive to settings load. */
  readonly entitlements = computed<ReadonlySet<string>>(() => {
    const granted = new Set<string>();
    for (const [key, value] of Object.entries(this.settings.settings().values)) {
      if (key.startsWith(ENTITLEMENT_PREFIX) && value === true) {
        granted.add(key.slice(ENTITLEMENT_PREFIX.length));
      }
    }
    return granted;
  });

  /** Accepts either a bare key (`payouts`) or a namespaced key (`entitlement.payouts`). */
  has(key: string): boolean {
    const normalized = key.startsWith(ENTITLEMENT_PREFIX)
      ? key.slice(ENTITLEMENT_PREFIX.length)
      : key;
    return this.entitlements().has(normalized);
  }
}
