import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import { ApiResponse, unwrapApiResponse } from '@tch/api';

export type TenantType = 'BORLETTE' | 'RESEAU' | 'AMBULANT';
export type TenantProvisioningProfile = 'MINIMAL' | 'DEFAULT_HAITI_LOTTERY' | 'DEMO';

/** Mirror of backend `TenantProvisioningRequest`. */
export interface TenantProvisioningRequest {
  readonly code: string;
  readonly name: string;
  readonly type: TenantType;
  readonly timezone: string;
  readonly currency: string;
  readonly profile: TenantProvisioningProfile;
  readonly initialAdminEmail?: string;
}

/** Mirror of backend `TenantProvisioningPreviewView`. */
export interface TenantProvisioningPreviewView {
  readonly profile: TenantProvisioningProfile;
  readonly includedDomains: readonly string[];
  readonly warnings: readonly string[];
  readonly notCopiedData: readonly string[];
  readonly expectedReadinessSections: readonly string[];
}

/** Mirror of backend `TenantProvisioningResultView` (subset consumed by the UI). */
export interface TenantProvisioningResultView {
  readonly tenantId: string;
  readonly tenantCode: string;
  readonly profile: TenantProvisioningProfile;
  readonly domainStatuses: Readonly<Record<string, string>>;
  readonly nextSteps: readonly string[];
  readonly warnings: readonly string[];
  readonly initialAdminUserId?: string;
}

/** SUPER_ADMIN tenant onboarding actions. */
@Injectable({ providedIn: 'root' })
export class PlatformAdminApi {
  private readonly http = inject(HttpClient);

  previewTenant(request: TenantProvisioningRequest): Observable<TenantProvisioningPreviewView> {
    return this.http
      .post<
        ApiResponse<TenantProvisioningPreviewView>
      >('/api/v1/platform/tenant-onboarding/preview', request)
      .pipe(map(unwrapApiResponse));
  }

  provisionTenant(request: TenantProvisioningRequest): Observable<TenantProvisioningResultView> {
    return this.http
      .post<
        ApiResponse<TenantProvisioningResultView>
      >('/api/v1/platform/tenant-onboarding/provision', request)
      .pipe(map(unwrapApiResponse));
  }
}
