import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

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

export interface PlatformSuperAdminView {
  readonly id: string;
  readonly email: string;
  readonly displayName: string;
  readonly status: string;
  readonly assignedAt: string;
}

export interface CreatePlatformSuperAdminRequest {
  readonly email: string;
  readonly displayName: string;
  readonly phoneNumber?: string | null;
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

  getSuperAdmin(userId: string): Observable<PlatformSuperAdminView> {
    return this.backend.get<{ id: string; email: string; displayName: string; status: string; createdAt: string }>(
      `/admin/identity/users/${userId}`,
    ).pipe(map(u => ({ id: u.id, email: u.email, displayName: u.displayName, status: u.status, assignedAt: u.createdAt })));
  }

  listSuperAdmins(): Observable<PlatformSuperAdminView[]> {
    return this.backend.get<PlatformSuperAdminView[]>('/platform/super-admins');
  }

  createSuperAdmin(request: CreatePlatformSuperAdminRequest): Observable<PlatformSuperAdminView> {
    return this.backend.post<PlatformSuperAdminView>('/platform/super-admins', request);
  }

  revokeSuperAdmin(userId: string): Observable<void> {
    return this.backend.delete<void>(`/platform/super-admins/${userId}`);
  }
}
