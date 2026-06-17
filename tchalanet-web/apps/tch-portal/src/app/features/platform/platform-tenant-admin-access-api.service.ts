import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import { Observable } from 'rxjs';

export interface StartTenantAdminAccessRequest {
  reason: string;
  mode: 'SUPPORT_OVERRIDE' | 'SUPPORT_READONLY';
}

export interface TenantAdminAccessSession {
  sessionId: string;
  tenantId: string;
  tenantCode: string;
  tenantName: string;
  startedAt: string;
  expiresAt?: string | null;
  actorRole: 'SUPER_ADMIN';
  mode: 'SUPPORT_OVERRIDE' | 'SUPPORT_READONLY';
  sensitiveDataMasked: boolean;
}

@Injectable({ providedIn: 'root' })
export class PlatformTenantAdminAccessApi {
  private readonly backend = inject(TchBackendClient);

  startAdminAccess(
    tenantId: string,
    req: StartTenantAdminAccessRequest,
  ): Observable<TenantAdminAccessSession> {
    return this.backend.post<TenantAdminAccessSession>(
      `/platform/tenants/${tenantId}/admin-access`,
      req,
    );
  }

  stopAdminAccess(): Observable<void> {
    return this.backend.delete<void>('/platform/tenants/admin-access/current');
  }
}
