import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import type { TchRequestOptions } from '@tch/api';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

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

interface IdentityUserResponse {
  readonly id: string;
  readonly email: string;
  readonly displayName: string;
  readonly status: string;
  readonly createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class PlatformSuperAdminsApi {
  private readonly backend = inject(TchBackendClient);

  getSuperAdmin(userId: string, options?: TchRequestOptions): Observable<PlatformSuperAdminView> {
    return this.backend.get<IdentityUserResponse>(
      `/admin/identity/users/${userId}`,
      options,
    ).pipe(map(toPlatformSuperAdminView));
  }

  listSuperAdmins(options?: TchRequestOptions): Observable<PlatformSuperAdminView[]> {
    return this.backend.get<PlatformSuperAdminView[]>('/platform/super-admins', options);
  }

  createSuperAdmin(
    request: CreatePlatformSuperAdminRequest,
    options?: TchRequestOptions,
  ): Observable<PlatformSuperAdminView> {
    return this.backend.post<PlatformSuperAdminView>('/platform/super-admins', request, options);
  }

  revokeSuperAdmin(userId: string, options?: TchRequestOptions): Observable<void> {
    return this.backend.delete<void>(`/platform/super-admins/${userId}`, options);
  }
}

function toPlatformSuperAdminView(user: IdentityUserResponse): PlatformSuperAdminView {
  return {
    id: user.id,
    email: user.email,
    displayName: user.displayName,
    status: user.status,
    assignedAt: user.createdAt,
  };
}
