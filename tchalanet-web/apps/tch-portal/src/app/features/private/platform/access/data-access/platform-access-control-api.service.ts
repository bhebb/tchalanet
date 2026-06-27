import { Injectable, inject } from '@angular/core';
import { TchBackendClient, TchPage } from '@tch/api';
import { Observable } from 'rxjs';

export interface AccessRoleView {
  readonly id: string;
  readonly code: string;
  readonly name: string | null;
  readonly description: string | null;
  readonly tenantId: string | null;
  readonly parentRoleId: string | null;
  readonly system: boolean;
}

export interface AccessPermissionView {
  readonly code: string;
  readonly name: string | null;
  readonly category: string | null;
  readonly description: string | null;
}

export interface EffectivePermissionsView {
  readonly tenantId: string;
  readonly userId: string;
  readonly roleIds: readonly string[];
  readonly permissionCodes: readonly string[];
}

export interface AccessUserView {
  readonly id: string;
  readonly username: string | null;
  readonly email: string | null;
  readonly phone: string | null;
  readonly status: string;
  readonly role: string | null;
  readonly membershipStatus: string | null;
  readonly createdAt: string | null;
  readonly firstName: string | null;
  readonly lastName: string | null;
  readonly displayName: string | null;
  readonly tenantId: string | null;
  readonly tenantName: string | null;
  readonly tenantCode: string | null;
}

export interface OverrideReasonRequest {
  readonly reason?: string | null;
}

@Injectable({ providedIn: 'root' })
export class PlatformAccessControlApi {
  private readonly backend = inject(TchBackendClient);

  listRoles(): Observable<AccessRoleView[]> {
    return this.backend.get<AccessRoleView[]>('/admin/access-control/roles');
  }

  listPermissions(): Observable<AccessPermissionView[]> {
    return this.backend.get<AccessPermissionView[]>('/admin/access-control/permissions');
  }

  listRolePermissions(roleId: string): Observable<string[]> {
    return this.backend.get<string[]>(
      `/admin/access-control/roles/${encodeURIComponent(roleId)}/permissions`,
    );
  }

  searchUsers(params: { q?: string; page?: number; size?: number } = {}): Observable<TchPage<AccessUserView>> {
    const query = new URLSearchParams({
      page: String(params.page ?? 0),
      size: String(params.size ?? 10),
    });
    if (params.q?.trim()) query.set('q', params.q.trim());
    return this.backend.get<TchPage<AccessUserView>>(`/admin/identity/users?${query.toString()}`);
  }

  getEffectivePermissions(userId: string): Observable<EffectivePermissionsView> {
    return this.backend.get<EffectivePermissionsView>(
      `/admin/access-control/users/${encodeURIComponent(userId)}/permissions/effective`,
    );
  }

  assignRole(userId: string, roleCode: string): Observable<void> {
    return this.backend.post<void>(
      `/admin/access-control/users/${encodeURIComponent(userId)}/roles/${encodeURIComponent(roleCode)}`,
      {},
    );
  }

  removeRole(userId: string, roleCode: string): Observable<void> {
    return this.backend.delete<void>(
      `/admin/access-control/users/${encodeURIComponent(userId)}/roles/${encodeURIComponent(roleCode)}`,
    );
  }

  grantPermission(userId: string, permissionCode: string, reason: string | null): Observable<void> {
    return this.backend.put<void, OverrideReasonRequest>(
      `/admin/access-control/users/${encodeURIComponent(userId)}/permissions/${encodeURIComponent(permissionCode)}/grant`,
      { reason },
    );
  }

  denyPermission(userId: string, permissionCode: string, reason: string | null): Observable<void> {
    return this.backend.put<void, OverrideReasonRequest>(
      `/admin/access-control/users/${encodeURIComponent(userId)}/permissions/${encodeURIComponent(permissionCode)}/deny`,
      { reason },
    );
  }

  removePermissionOverride(userId: string, permissionCode: string): Observable<void> {
    return this.backend.delete<void>(
      `/admin/access-control/users/${encodeURIComponent(userId)}/permissions/${encodeURIComponent(permissionCode)}/override`,
    );
  }
}
