import { Injectable, inject } from '@angular/core';
import { ApiResponse, TchBackendClient, TchRequestOptions } from '@tch/api';
import type { ConsoleSubscriptionStatus } from '@tch/web/console';
import { Observable } from 'rxjs';

export type SubscriptionStatus = ConsoleSubscriptionStatus;

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

  cancel(reason?: string, options?: TchRequestOptions): Observable<ApiResponse<unknown>> {
    return this.backend.postApiResponse<unknown>(
      '/tenant/subscription/cancel',
      reason ? { reason } : {},
      options,
    );
  }

  renew(newEndsAt: string, options?: TchRequestOptions): Observable<ApiResponse<unknown>> {
    return this.backend.postApiResponse<unknown>('/tenant/subscription/renew', { newEndsAt }, options);
  }

  resume(options?: TchRequestOptions): Observable<ApiResponse<unknown>> {
    return this.backend.postApiResponse<unknown>('/tenant/subscription/resume', {}, options);
  }

  suspend(options?: TchRequestOptions): Observable<ApiResponse<unknown>> {
    return this.backend.postApiResponse<unknown>('/tenant/subscription/suspend', {}, options);
  }
}
