import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import type { TchRequestOptions } from '@tch/api';
import { Observable } from 'rxjs';

export interface CreateIdentityUserRequest {
  readonly email: string;
  readonly displayName: string;
  readonly phoneNumber?: string | null;
}

export interface AssignMembershipRequest {
  readonly tenantId: string;
  readonly role: string;
}

export interface IdentityUserView {
  readonly id: string;
  readonly email: string | null;
  readonly displayName: string | null;
  readonly status: string;
}

export interface PasswordResetResult {
  readonly tempPassword: string;
}

@Injectable({ providedIn: 'root' })
export class IdentityUserCrudApi {
  private readonly backend = inject(TchBackendClient);

  searchUnassigned(q: string, page = 0, size = 10, options?: TchRequestOptions): Observable<IdentityUserView[]> {
    const params = new URLSearchParams({ unassigned: 'true', page: String(page), size: String(size) });
    if (q?.trim()) params.set('q', q.trim());
    return this.backend.get<IdentityUserView[]>(`/identity/users?${params.toString()}`, options);
  }

  createUser(request: CreateIdentityUserRequest, options?: TchRequestOptions): Observable<IdentityUserView> {
    return this.backend.post<IdentityUserView>('/identity/users', request, options);
  }

  assignMembership(userId: string, request: AssignMembershipRequest, options?: TchRequestOptions): Observable<void> {
    return this.backend.post<void>(`/identity/users/${userId}/membership`, request, options);
  }

  activate(userId: string, options?: TchRequestOptions): Observable<void> {
    return this.backend.post<void>(`/identity/users/${userId}/activate`, {}, options);
  }

  suspend(userId: string, options?: TchRequestOptions): Observable<void> {
    return this.backend.post<void>(`/identity/users/${userId}/suspend`, {}, options);
  }

  archive(userId: string, options?: TchRequestOptions): Observable<void> {
    return this.backend.delete<void>(`/identity/users/${userId}`, options);
  }

  resetPassword(userId: string, options?: TchRequestOptions): Observable<PasswordResetResult> {
    return this.backend.post<PasswordResetResult>(`/identity/users/${userId}/reset-password`, {}, options);
  }
}
