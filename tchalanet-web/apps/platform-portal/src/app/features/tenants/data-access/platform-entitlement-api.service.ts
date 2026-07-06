import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import { Observable } from 'rxjs';

import { TenantCapabilitySnapshot } from './platform-tenant-contracts';

@Injectable({ providedIn: 'root' })
export class PlatformEntitlementApi {
  private readonly backend = inject(TchBackendClient);

  getSnapshot(tenantId: string): Observable<TenantCapabilitySnapshot> {
    return this.backend.get<TenantCapabilitySnapshot>(`/platform/entitlements/${tenantId}`);
  }
}
