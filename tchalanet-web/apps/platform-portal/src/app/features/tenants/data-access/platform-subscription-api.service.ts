import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import { Observable } from 'rxjs';

import {
  ApplyPlanResult,
  PlanSummaryView,
  SubscriptionView,
} from './platform-tenant-contracts';

@Injectable({ providedIn: 'root' })
export class PlatformSubscriptionApi {
  private readonly backend = inject(TchBackendClient);

  /** Returns null when the tenant has no subscription applied yet. */
  resolve(tenantId: string): Observable<SubscriptionView | null> {
    return this.backend.get<SubscriptionView | null>(`/platform/subscriptions/${tenantId}`);
  }

  /** Active plans a super-admin can assign to a tenant or during provisioning. */
  listActivePlans(): Observable<PlanSummaryView[]> {
    return this.backend.get<PlanSummaryView[]>('/platform/plans');
  }

  apply(tenantId: string, planCode: string): Observable<ApplyPlanResult> {
    return this.backend.post<ApplyPlanResult>(`/platform/subscriptions/${tenantId}/apply`, { planCode });
  }
}
