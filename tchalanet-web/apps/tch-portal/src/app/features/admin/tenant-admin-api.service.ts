import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import { ApiResponse } from '../../shared/types';
import { unwrapApiResponse } from '../../core/http';

/**
 * Mirror of backend admin `CreateUserRequest`. The tenant is resolved server-side from request
 * context — there is no tenant id field here by design. A seller is created with `role=CASHIER`,
 * which requires `outletId`.
 */
export interface CreateUserRequest {
  readonly email: string;
  readonly phone?: string;
  readonly firstName?: string;
  readonly lastName?: string;
  readonly role: 'CASHIER';
  readonly outletId: string;
  readonly terminalId?: string;
}

/** Subset of the created-user response the UI confirms against. */
export interface CreatedUserView {
  readonly id?: string;
  readonly email?: string;
}

/** TENANT_ADMIN seller onboarding action. */
@Injectable({ providedIn: 'root' })
export class TenantAdminApi {
  private readonly http = inject(HttpClient);

  createSeller(request: CreateUserRequest): Observable<CreatedUserView> {
    return this.http
      .post<ApiResponse<CreatedUserView>>('/api/v1/admin/identity/users', request)
      .pipe(map(unwrapApiResponse));
  }
}
