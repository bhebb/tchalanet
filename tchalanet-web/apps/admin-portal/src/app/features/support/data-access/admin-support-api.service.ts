import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiResponse, TchBackendClient } from '@tch/api';

export type AdminSupportContactIntent = 'SUPPORT';

export interface AdminSupportRequest {
  readonly intent: AdminSupportContactIntent;
  readonly fullName: string;
  readonly phone: string;
  readonly email?: string;
  readonly organizationName?: string;
  readonly city?: string;
  readonly country?: string;
  readonly message: string;
  readonly consentToContact: boolean;
  readonly sourcePage?: string;
}

export interface AdminSupportSubmittedResponse {
  readonly requestId: string;
  readonly status: 'RECEIVED';
  readonly message: string;
}

@Injectable({ providedIn: 'root' })
export class AdminSupportApi {
  private readonly backend = inject(TchBackendClient);

  submit(request: AdminSupportRequest): Observable<ApiResponse<AdminSupportSubmittedResponse>> {
    return this.backend.postApiResponse<AdminSupportSubmittedResponse, AdminSupportRequest>(
      '/public/contact-requests',
      request,
      { suppressShellFeedback: true },
    );
  }
}
