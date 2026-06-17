import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import { Observable } from 'rxjs';

export type TenantStatus = 'DRAFT' | 'ACTIVE' | 'SUSPENDED' | 'ARCHIVED';
export type TenantType = 'BORLETTE' | 'RESEAU' | 'AMBULANT';

export interface TenantSummaryView {
  id: string;
  code: string;
  name: string;
  type: TenantType;
  timezone: string;
  currency: string;
  status: TenantStatus;
  createdAt: string;
}

export interface TenantPageResponse {
  items: TenantSummaryView[];
  total: number;
  page: number;
  size: number;
  totalPages: number;
}

export interface CreateTenantRequest {
  code: string;
  name: string;
  type: TenantType;
  timezone: string;
  currency: string;
  activeThemeId?: string | null;
  activate?: boolean;
  address?: {
    country?: string;
    city?: string;
    line1?: string;
    line2?: string;
    postalCode?: string;
  } | null;
}

export interface CreateTenantAdminRequest {
  email: string;
  displayName: string;
  phoneNumber?: string | null;
  roleCodes: string[];
  sendInvite: boolean;
}

export interface TenantAdminView {
  id: string;
  email: string;
  displayName: string;
  roleCodes: string[];
  status: string;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class PlatformTenantsApi {
  private readonly backend = inject(TchBackendClient);

  listTenants(params: { page: number; size: number }): Observable<TenantPageResponse> {
    return this.backend.get<TenantPageResponse>(
      `/platform/tenants?page=${params.page}&size=${params.size}`,
    );
  }

  getTenant(id: string): Observable<TenantSummaryView> {
    return this.backend.get<TenantSummaryView>(`/platform/tenants/${id}`);
  }

  createTenant(req: CreateTenantRequest): Observable<TenantSummaryView> {
    return this.backend.post<TenantSummaryView>('/platform/tenants', req);
  }

  activateTenant(id: string): Observable<void> {
    return this.backend.post<void>(`/platform/tenants/${id}/activate`, {});
  }

  suspendTenant(id: string): Observable<void> {
    return this.backend.post<void>(`/platform/tenants/${id}/suspend`, {});
  }

  archiveTenant(id: string): Observable<void> {
    return this.backend.post<void>(`/platform/tenants/${id}/archive`, {});
  }

  reactivateTenant(id: string): Observable<void> {
    return this.backend.post<void>(`/platform/tenants/${id}/reactivate`, {});
  }

  listTenantAdmins(tenantId: string): Observable<TenantAdminView[]> {
    return this.backend.get<TenantAdminView[]>(`/platform/tenants/${tenantId}/admins`);
  }

  createTenantAdmin(tenantId: string, req: CreateTenantAdminRequest): Observable<TenantAdminView> {
    return this.backend.post<TenantAdminView>(`/platform/tenants/${tenantId}/admins`, req);
  }
}
