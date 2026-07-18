import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import { Observable } from 'rxjs';

import {
  TenantProvisioningProfile,
  TenantReadinessView,
  TenantType,
} from './platform-tenant-contracts';

export interface TenantProvisioningRequest {
  code: string;
  name: string;
  type: TenantType;
  timezone: string;
  currency: string;
  /** Default seller-terminal commission rate (percent, 0–100). */
  defaultCommissionRate: number;
  profile: TenantProvisioningProfile;
  maryajGratisEnabled?: boolean | null;
  initialAdminUsername: string;
  initialAdminEmail: string;
  planCode?: string | null;
}

export interface TenantProvisioningPreviewView {
  profile: TenantProvisioningProfile;
  includedDomains: string[];
  warnings: string[];
  notCopiedData: string[];
  expectedReadinessSections: string[];
}

export interface TenantProvisioningDomainStatuses {
  tenant_identity: string;
  pagemodels: string;
  theme: string;
  settings: string;
  i18n: string;
  games: string;
  pricing: string;
  draw_channels: string;
  promotions_templates: string;
  limits_templates: string;
  subscription: string;
}

export interface TenantProvisioningResultView {
  tenantId: string;
  tenantCode: string;
  profile: TenantProvisioningProfile;
  defaultCommissionRate?: number;
  domainStatuses: TenantProvisioningDomainStatuses;
  nextSteps: string[];
  warnings: string[];
  appliedPlanCode?: string | null;
  readiness: TenantReadinessView;
  initialAdminUserId?: string | null;
  initialAdminUsername?: string | null;
  initialAdminEmail?: string | null;
  initialAdminCredentialStatus?: string | null;
  initialAdminTemporaryPassword?: string | null;
  initialAdminMustChangePassword?: boolean | null;
  initialAdminMustCompleteProfile?: boolean | null;
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
