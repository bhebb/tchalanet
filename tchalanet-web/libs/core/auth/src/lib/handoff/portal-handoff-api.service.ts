import { Injectable, inject } from '@angular/core';
import { TchBackendClient, TchRequestOptions } from '@tch/api';
import { Observable } from 'rxjs';

export type PortalHandoffTarget = 'ADMIN' | 'PLATFORM';

export interface CreatePortalHandoffRequest {
  readonly targetPortal: PortalHandoffTarget;
  readonly entryRoute: string;
  readonly supportAccessSessionId?: string | null;
}

export interface CreatePortalHandoffResponse {
  readonly handoffId: string;
  readonly code: string;
  readonly targetPortal: PortalHandoffTarget;
  readonly targetUrl: string;
  readonly entryRoute: string;
  readonly expiresAt: string;
}

export interface ConsumePortalHandoffResponse {
  readonly customToken: string;
  readonly targetPortal: PortalHandoffTarget;
  readonly entryRoute: string;
  readonly supportAccessSessionId?: string | null;
}

@Injectable({ providedIn: 'root' })
export class PortalHandoffApi {
  private readonly backend = inject(TchBackendClient);

  create(
    request: CreatePortalHandoffRequest,
    options?: TchRequestOptions,
  ): Observable<CreatePortalHandoffResponse> {
    return this.backend.post<CreatePortalHandoffResponse>(
      '/platform/auth/portal-handoffs',
      request,
      options,
    );
  }

  consume(
    handoffId: string,
    code: string,
    targetPortal: PortalHandoffTarget,
    options?: TchRequestOptions,
  ): Observable<ConsumePortalHandoffResponse> {
    return this.backend.post<ConsumePortalHandoffResponse>(
      `/platform/auth/portal-handoffs/${handoffId}/consume`,
      { code, targetPortal },
      options,
    );
  }
}
