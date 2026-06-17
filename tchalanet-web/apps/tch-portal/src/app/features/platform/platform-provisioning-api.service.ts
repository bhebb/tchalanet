import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import { Observable } from 'rxjs';

export type TenantProvisioningProfile = 'MINIMAL' | 'DEFAULT_HAITI_LOTTERY' | 'DEMO';
export type TenantType = 'BORLETTE' | 'RESEAU' | 'AMBULANT';

export interface TenantProvisioningRequest {
  code: string;
  name: string;
  type: TenantType;
  timezone: string;
  currency: string;
  profile: TenantProvisioningProfile;
  initialAdminEmail?: string | null;
}

export interface TenantProvisioningPreviewView {
  profile: TenantProvisioningProfile;
  includedDomains: string[];
  warnings: string[];
  notCopiedData: string[];
  expectedReadinessSections: string[];
}

export interface TenantProvisioningResultView {
  tenantId: string;
  tenantCode: string;
  profile: TenantProvisioningProfile;
  domainStatuses: Record<string, string>;
  nextSteps: string[];
  warnings: string[];
  readiness: {
    status: 'READY' | 'INCOMPLETE' | 'MISSING' | 'UNKNOWN';
    missingCount: number;
    sections: Array<{ id: string; labelKey: string; status: string; route: string; issues: unknown[] }>;
  };
  initialAdminUserId?: string | null;
}

@Injectable({ providedIn: 'root' })
export class PlatformProvisioningApi {
  private readonly backend = inject(TchBackendClient);

  preview(req: TenantProvisioningRequest): Observable<TenantProvisioningPreviewView> {
    return this.backend.post<TenantProvisioningPreviewView>(
      '/platform/tenant-onboarding/preview',
      req,
    );
  }

  provision(req: TenantProvisioningRequest): Observable<TenantProvisioningResultView> {
    return this.backend.post<TenantProvisioningResultView>(
      '/platform/tenant-onboarding/provision',
      req,
    );
  }
}
