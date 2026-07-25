import { Injectable, inject } from '@angular/core';
import { TchBackendClient, TchRequestOptions } from '@tch/api';
import { Observable } from 'rxjs';

export interface UpdateTenantIdentityRequest {
  readonly name: string;
  readonly displayName?: string | null;
  readonly timezone: string;
  readonly currency: string;
}

export interface UpsertAddressRequest {
  readonly line1: string;
  readonly line2?: string | null;
  readonly city: string;
  readonly region?: string | null;
  readonly country: string;
  readonly postalCode?: string | null;
}

@Injectable({ providedIn: 'root' })
export class TenantGeneralConfigApiService {
  private readonly backend = inject(TchBackendClient);

  updateIdentity(req: UpdateTenantIdentityRequest, options?: TchRequestOptions): Observable<void> {
    return this.backend.put<void>('/admin/tenant', req, options);
  }

  upsertAddress(req: UpsertAddressRequest, options?: TchRequestOptions): Observable<void> {
    return this.backend.put<void>('/admin/tenant/address', req, options);
  }
}
