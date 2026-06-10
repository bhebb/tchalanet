import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import { Observable } from 'rxjs';

export interface CreateUserRequest {
  readonly email: string;
  readonly phone?: string;
  readonly firstName?: string;
  readonly lastName?: string;
  readonly role: 'CASHIER';
  readonly outletId: string;
  readonly terminalId?: string;
}

export interface CreatedUserView {
  readonly id?: string;
  readonly email?: string;
}

@Injectable({ providedIn: 'root' })
export class TenantAdminApi {
  private readonly backend = inject(TchBackendClient);

  createSeller(request: CreateUserRequest): Observable<CreatedUserView> {
    return this.backend.post<CreatedUserView>('/admin/identity/users', request);
  }
}
