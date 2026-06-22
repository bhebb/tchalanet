import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import { Observable, throwError } from 'rxjs';

export type TenantStatus = 'DRAFT' | 'ACTIVE' | 'SUSPENDED' | 'REJECTED' | 'ARCHIVED';
export type TenantType = 'BORLETTE' | 'RESEAU' | 'AMBULANT';
export type TenantProvisioningProfile = 'MINIMAL' | 'DEFAULT_HAITI_LOTTERY' | 'DEMO';
export type TenantReadinessStatus = 'READY' | 'INCOMPLETE' | 'BLOCKED' | 'MISSING' | 'UNKNOWN';

export interface TenantSummaryView {
  id?: string;
  tenantId?: string;
  code: string;
  name: string;
  type?: TenantType | string | null;
  timezone?: string | null;
  currency?: string | null;
  status: TenantStatus;
  defaultCommissionRate?: number | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

/** Full tenant view returned by GET /platform/tenants/:id */
export interface TenantDetailView extends TenantSummaryView {
  profile?: TenantProvisioningProfile | null;
  primaryAdminEmail?: string | null;
  readinessStatus?: TenantReadinessStatus | null;
  activeThemeId?: string | null;
  themeCode?: string | null;
  address?: {
    country?: string | null;
    city?: string | null;
    line1?: string | null;
    line2?: string | null;
    postalCode?: string | null;
  } | null;
}

export interface TenantPageResponse {
  items: TenantSummaryView[];
  total: number;
  page: number;
  size: number;
  totalPages: number;
}

export interface TenantListQuery {
  q?: string | null;
  status?: string | null;
  page: number;
  size: number;
  sort?: string | null;
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

  listTenants(params: TenantListQuery): Observable<TenantPageResponse> {
    const query = new URLSearchParams();
    query.set('page', String(params.page));
    query.set('size', String(params.size));
    if (params.q?.trim()) query.set('q', params.q.trim());
    if (params.status) query.set('status', params.status);
    if (params.sort) query.set('sort', params.sort);
    return this.backend.get<TenantPageResponse>(`/platform/tenants?${query.toString()}`);
  }

  getTenant(id: string): Observable<TenantDetailView> {
    // Guard against missing route params: never hit `/platform/tenants/null` — fail fast
    // client-side so the caller shows a clear "no tenant selected" state instead of a 500.
    if (!isValidId(id)) {
      return throwError(() => new Error('Aucun tenant sélectionné.'));
    }
    return this.backend.get<TenantDetailView>(`/platform/tenants/${id}`);
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

/** A usable path segment: not empty and not a stringified null/undefined from a missing route param. */
function isValidId(id: string | null | undefined): id is string {
  return !!id && id !== 'null' && id !== 'undefined';
}
