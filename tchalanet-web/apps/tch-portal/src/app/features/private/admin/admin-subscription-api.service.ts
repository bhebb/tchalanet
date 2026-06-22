import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
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

  get(): Observable<SubscriptionView> {
    return this.backend.get<SubscriptionView>('/tenant/subscription');
  }

  cancel(reason?: string): Observable<void> {
    return this.backend.post<void>('/tenant/subscription/cancel', reason ? { reason } : {});
  }

  renew(newEndsAt: string): Observable<void> {
    return this.backend.post<void>('/tenant/subscription/renew', { newEndsAt });
  }

  resume(): Observable<void> {
    return this.backend.post<void>('/tenant/subscription/resume', {});
  }

  suspend(): Observable<void> {
    return this.backend.post<void>('/tenant/subscription/suspend', {});
  }
}
