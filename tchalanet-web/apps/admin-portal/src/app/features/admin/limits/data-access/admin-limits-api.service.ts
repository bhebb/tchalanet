import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import type { TchRequestOptions } from '@tch/api';
import { Observable } from 'rxjs';

import type {
  LimitRuleSpec,
  ListLimitAssignmentsView,
  TargetType,
  UpsertLimitAssignmentRequest,
} from './admin-limits.models';

@Injectable({ providedIn: 'root' })
export class AdminLimitsApi {
  private readonly backend = inject(TchBackendClient);

  listRules(options?: TchRequestOptions): Observable<LimitRuleSpec[]> {
    return this.backend.get<LimitRuleSpec[]>('/admin/policies/limits/rules', options);
  }

  listAssignments(
    target: TargetType,
    targetId?: string,
    options?: TchRequestOptions,
  ): Observable<ListLimitAssignmentsView> {
    const qs = new URLSearchParams({ target });
    if (targetId) qs.set('targetId', targetId);
    return this.backend.get<ListLimitAssignmentsView>(`/admin/policies/limits/assignments?${qs}`, options);
  }

  upsertAssignment(
    req: UpsertLimitAssignmentRequest,
    options?: TchRequestOptions,
  ): Observable<{ id: { value: string } }> {
    return this.backend.put<{ id: { value: string } }>('/admin/policies/limits/assignments', req, options);
  }

  deleteAssignment(id: string, options?: TchRequestOptions): Observable<unknown> {
    return this.backend.delete<unknown>(`/admin/policies/limits/assignments/${id}`, options);
  }
}

// Re-export types consumed by pages/components that import from this service
export type {
  LimitRuleSpec,
  LimitAssignmentItem,
  ListLimitAssignmentsView,
  TargetType,
  UpsertLimitAssignmentRequest,
  BreachOutcome,
  RuleKey,
  RuleRow,
} from './admin-limits.models';
