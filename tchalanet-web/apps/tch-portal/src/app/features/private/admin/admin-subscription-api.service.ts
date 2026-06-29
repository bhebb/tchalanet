import { Injectable, inject } from '@angular/core';
import { TchBackendClient, TchRequestOptions } from '@tch/api';
import { Observable } from 'rxjs';

export type SubscriptionStatus =
  | 'ACTIVE'
  | 'TRIALING'
  | 'PAST_DUE'
  | 'CANCELLED'
  | 'SUSPENDED'
  | 'EXPIRED';

export interface SubscriptionView {
  tenantId: string;
  planCode: string;
  status: SubscriptionStatus;
  startedAt: string;
  endsAt?: string | null;
  version: number;
  updatedAt: string;
}

@Injectable({ providedIn: 'root' })
export class AdminSubscriptionApi {
  private readonly backend = inject(TchBackendClient);

  get(options?: TchRequestOptions): Observable<SubscriptionView> {
    return this.backend.get<SubscriptionView>('/tenant/subscription', options);
  }

  cancel(reason?: string, options?: TchRequestOptions): Observable<void> {
    return this.backend.post<void>('/tenant/subscription/cancel', reason ? { reason } : {}, options);
  }

  renew(newEndsAt: string, options?: TchRequestOptions): Observable<void> {
    return this.backend.post<void>('/tenant/subscription/renew', { newEndsAt }, options);
  }

  resume(options?: TchRequestOptions): Observable<void> {
    return this.backend.post<void>('/tenant/subscription/resume', {}, options);
  }

  suspend(options?: TchRequestOptions): Observable<void> {
    return this.backend.post<void>('/tenant/subscription/suspend', {}, options);
  }
}
