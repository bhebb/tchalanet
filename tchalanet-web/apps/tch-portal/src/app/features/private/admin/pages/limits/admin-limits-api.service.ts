import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import { Observable } from 'rxjs';

export type TargetType =
  | 'TENANT' | 'AGENT' | 'OUTLET' | 'TERMINAL'
  | 'SELLER_TERMINAL' | 'GAME' | 'ZONE' | 'RANGE' | 'DRAW_CHANNEL';

export type BreachOutcome = 'ALLOW' | 'WARN' | 'REQUIRE_APPROVAL' | 'BLOCK';

export type RuleKey =
  | 'MAX_STAKE_PER_LINE'
  | 'MAX_LINES_PER_TICKET'
  | 'MAX_STAKE_PER_TICKET'
  | 'MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW'
  | 'MAX_POTENTIAL_PAYOUT_EXPOSURE_PER_SELECTION_PER_DRAW'
  | 'MAX_STAKE_PER_BET_TYPE_PER_TICKET'
  | 'MAX_STAKE_PER_SELECTION_PER_TICKET'
  | 'MAX_POTENTIAL_PAYOUT_PER_TICKET'
  | 'MAX_POTENTIAL_PAYOUT_PER_LINE'
  | 'MAX_SALES_COUNT_PER_SELECTION_PER_DRAW'
  | 'MAX_SALES_COUNT_PER_TICKET'
  | 'BLOCK_SELECTION_PER_DRAW'
  | 'BLOCK_BET_TYPE'
  | 'MAX_TICKET_COUNT_PER_AGENT_PER_WINDOW'
  | 'MAX_STAKE_PER_AGENT_PER_DRAW'
  | 'MAX_STAKE_PER_OUTLET_PER_DRAW';

export interface LimitRuleSpec {
  ruleKey: RuleKey;
  label: string;
  description: string;
  defaultOutcome: BreachOutcome;
  category: string;
  stateless: boolean;
  paramsTemplate: unknown;
}

export interface LimitAssignmentItem {
  id: { value: string };
  ruleKey: RuleKey;
  enabled: boolean;
  onBreach: BreachOutcome;
  params: unknown;
  startsAt: string | null;
  endsAt: string | null;
}

export interface ListLimitAssignmentsView {
  limitScopeRef: unknown;
  items: LimitAssignmentItem[];
}

export interface UpsertLimitAssignmentRequest {
  ruleKey: RuleKey;
  targetType: TargetType;
  targetId?: string | null;
  enabled: boolean;
  onBreach: BreachOutcome;
  params: unknown;
  startsAt?: string | null;
  endsAt?: string | null;
}

@Injectable({ providedIn: 'root' })
export class AdminLimitsApi {
  private readonly backend = inject(TchBackendClient);

  listRules(): Observable<LimitRuleSpec[]> {
    return this.backend.get<LimitRuleSpec[]>('/admin/policies/limits/rules');
  }

  listAssignments(target: TargetType, targetId?: string): Observable<ListLimitAssignmentsView> {
    const qs = new URLSearchParams({ target });
    if (targetId) qs.set('targetId', targetId);
    return this.backend.get<ListLimitAssignmentsView>(`/admin/policies/limits/assignments?${qs}`);
  }

  upsertAssignment(req: UpsertLimitAssignmentRequest): Observable<{ id: { value: string } }> {
    return this.backend.put<{ id: { value: string } }>('/admin/policies/limits/assignments', req);
  }

  deleteAssignment(id: string): Observable<unknown> {
    return this.backend.delete<unknown>(`/admin/policies/limits/assignments/${id}`);
  }
}
