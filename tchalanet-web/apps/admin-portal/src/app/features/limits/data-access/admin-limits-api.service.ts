import { Injectable, Injector, ResourceRef, Signal, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { TchBackendClient } from '@tch/api';
import type { TchRequestOptions } from '@tch/api';
import { Observable, forkJoin, of } from 'rxjs';
import { map } from 'rxjs/operators';

import type {
  LimitAssignmentItem,
  LimitRuleSpec,
  ListLimitAssignmentsView,
  TargetType,
  TenantAdminPoliciesOverviewView,
  UpsertLimitAssignmentRequest,
} from './admin-limits.models';

export interface CombinedLimitData {
  specs: LimitRuleSpec[];
  assignments: LimitAssignmentItem[];
  inheritedAssignments: LimitAssignmentItem[] | null;
}

@Injectable({ providedIn: 'root' })
export class AdminLimitsApi {
  private readonly backend = inject(TchBackendClient);
  private readonly injector = inject(Injector);

  listRules(options?: TchRequestOptions): Observable<LimitRuleSpec[]> {
    return this.backend.get<LimitRuleSpec[]>('/admin/policies/limits/rules', options);
  }

  overview(options?: TchRequestOptions): Observable<TenantAdminPoliciesOverviewView> {
    return this.backend.get<TenantAdminPoliciesOverviewView>('/admin/policies/overview', options);
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

  /**
   * Resource combinant règles + assignments pour un scope cible, avec héritage optionnel.
   * Appelé lors de l'initialisation d'un composant (contexte d'injection) ; les InputSignal
   * passés comme params pilotent le re-fetch automatique.
   */
  combinedLimitsResource(params: {
    targetType: Signal<TargetType>;
    targetId: Signal<string | null>;
    inheritedTargetType: Signal<TargetType | null>;
    inheritedTargetId: Signal<string | null>;
    preloadedData?: Signal<CombinedLimitData | null>;
  }): ResourceRef<CombinedLimitData | undefined> {
    return rxResource<CombinedLimitData, readonly [TargetType, string | null, TargetType | null, string | null, CombinedLimitData | null]>({
      injector: this.injector,
      params: () => [
        params.targetType(),
        params.targetId(),
        params.inheritedTargetType(),
        params.inheritedTargetId(),
        params.preloadedData?.() ?? null,
      ],
      stream: ({ params: p }) => {
        const [target, targetId, inheritedType, inheritedId, preloaded] = p;
        if (preloaded) {
          return of(preloaded);
        }
        const opts: TchRequestOptions = { suppressShellFeedback: true };

        if (inheritedType) {
          return forkJoin([
            this.listRules(opts),
            this.listAssignments(target, targetId ?? undefined, opts),
            this.listAssignments(inheritedType, inheritedId ?? undefined, opts),
          ]).pipe(
            map(([specs, current, inherited]) => ({
              specs,
              assignments: current.items,
              inheritedAssignments: inherited.items,
            })),
          );
        }

        return forkJoin([
          this.listRules(opts),
          this.listAssignments(target, targetId ?? undefined, opts),
        ]).pipe(
          map(([specs, current]) => ({
            specs,
            assignments: current.items,
            inheritedAssignments: null,
          })),
        );
      },
    });
  }

  /**
   * Resource combinant règles + assignments TENANT (scope fixe, sans héritage).
   * Utilisée par la page des limites numériques.
   */
  tenantLimitsResource(): ResourceRef<{ specs: LimitRuleSpec[]; assignments: LimitAssignmentItem[] } | undefined> {
    const opts: TchRequestOptions = { suppressShellFeedback: true };
    return rxResource({
      injector: this.injector,
      params: () => 'tenant-limits' as const,
      stream: () =>
        forkJoin([
          this.listRules(opts),
          this.listAssignments('TENANT', undefined, opts),
        ]).pipe(
          map(([specs, view]) => ({ specs, assignments: view.items })),
        ),
    });
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
  TenantAdminPoliciesOverviewView,
} from './admin-limits.models';
