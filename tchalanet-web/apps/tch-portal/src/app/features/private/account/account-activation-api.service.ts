import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import { Observable } from 'rxjs';

export interface CompleteFirstLoginRequest {
  firstName: string;
  lastName: string;
  phoneNumber?: string | null;
  passwordChanged: boolean;
}

export interface CompleteFirstLoginResponse {
  userId: string;
  mustChangePassword: boolean;
  mustCompleteProfile: boolean;
  entryRoute: string;
}

@Injectable({ providedIn: 'root' })
export class AccountActivationApi {
  private readonly backend = inject(TchBackendClient);

  completeFirstLogin(req: CompleteFirstLoginRequest): Observable<CompleteFirstLoginResponse> {
    return this.backend.post<CompleteFirstLoginResponse>('/identity/me/complete-first-login', req);
  }
}
