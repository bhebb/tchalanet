import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import { Observable } from 'rxjs';

export type TenantType = 'BORLETTE' | 'RESEAU' | 'AMBULANT';
export type TenantProvisioningProfile = 'MINIMAL' | 'DEFAULT_HAITI_LOTTERY' | 'DEMO';

export interface TenantProvisioningRequest {
  readonly code: string;
  readonly name: string;
  readonly type: TenantType;
  readonly timezone: string;
  readonly currency: string;
  readonly profile: TenantProvisioningProfile;
  readonly initialAdminEmail?: string;
}

export interface TenantProvisioningPreviewView {
  readonly profile: TenantProvisioningProfile;
  readonly includedDomains: readonly string[];
  readonly warnings: readonly string[];
  readonly notCopiedData: readonly string[];
  readonly expectedReadinessSections: readonly string[];
}

export interface TenantProvisioningResultView {
  readonly tenantId: string;
  readonly tenantCode: string;
  readonly profile: TenantProvisioningProfile;
  readonly domainStatuses: Readonly<Record<string, string>>;
  readonly nextSteps: readonly string[];
  readonly warnings: readonly string[];
  readonly initialAdminUserId?: string;
}

@Injectable({ providedIn: 'root' })
export class PlatformAdminApi {
  private readonly backend = inject(TchBackendClient);

  previewTenant(request: TenantProvisioningRequest): Observable<TenantProvisioningPreviewView> {
    return this.backend.post<TenantProvisioningPreviewView>(
      '/platform/tenant-onboarding/preview',
      request,
    );
  }

  provisionTenant(request: TenantProvisioningRequest): Observable<TenantProvisioningResultView> {
    return this.backend.post<TenantProvisioningResultView>(
      '/platform/tenant-onboarding/provision',
      request,
    );
  }
}
