import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ResourceRef,
  computed,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { filter } from 'rxjs';

import { TchConfirmDialog } from '@tch/ui/components';
import { TchSectionError } from '@tch/ui/components';
import { AdminEmptyStateComponent } from '@tch/ui/console';
import { resourceErrorVm, TchAsyncReadyDirective, TchAsyncViewComponent, tchMutation } from '@tch/web/async';

import { AdminLimitsApi } from '../../data-access/admin-limits-api.service';
import type {
  LimitAssignmentItem,
  LimitRuleSpec,
  RuleKey,
  RuleRow,
} from '../../data-access/admin-limits.models';
import { LimitAssignmentsTableComponent } from '../../components/limit-assignments-table/limit-assignments-table.component';
import { UpsertLimitDialogComponent } from '../../components/upsert-limit-dialog/upsert-limit-dialog.component';
import { BlockNumberQuickDialogComponent } from '../../components/block-number-quick-dialog/block-number-quick-dialog.component';

const NUMBER_RULE_KEYS: RuleKey[] = [
  'MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW',
  'BLOCK_SELECTION_PER_DRAW',
];

interface NumberLimitAction {
  readonly ruleKey: RuleKey;
  readonly icon: string;
  readonly titleKey: string;
  readonly descriptionKey: string;
  readonly ctaKey: string;
}

const NUMBER_LIMIT_ACTIONS: readonly NumberLimitAction[] = [
  {
    ruleKey: 'BLOCK_SELECTION_PER_DRAW',
    icon: 'block',
    titleKey: 'admin.limits.number.actions.block.title',
    descriptionKey: 'admin.limits.number.actions.block.description',
    ctaKey: 'admin.limits.number.actions.block.cta',
  },
  {
    ruleKey: 'MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW',
    icon: 'payments',
    titleKey: 'admin.limits.number.actions.exposure.title',
    descriptionKey: 'admin.limits.number.actions.exposure.description',
    ctaKey: 'admin.limits.number.actions.exposure.cta',
  },
] satisfies readonly NumberLimitAction[];

@Component({
  selector: 'tch-admin-limits-number-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslatePipe,
    MatButtonModule,
    TchSectionError,
    TchAsyncViewComponent,
    TchAsyncReadyDirective,
    AdminEmptyStateComponent,
    LimitAssignmentsTableComponent,
  ],
  templateUrl: './admin-limits-number.page.html',
  styleUrl: './admin-limits-number.page.scss',
})
export class AdminLimitsNumberPage {
  private readonly api = inject(AdminLimitsApi);
  private readonly dialog = inject(MatDialog);
  private readonly translate = inject(TranslateService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly dataResource: ResourceRef<{ specs: LimitRuleSpec[]; assignments: LimitAssignmentItem[] } | undefined> =
    this.api.tenantLimitsResource();
  readonly loadError = resourceErrorVm(this.dataResource, 'admin.limits.number');
  readonly actionNotice = signal<string | null>(null);

  readonly deleteAssignment = tchMutation<string, unknown>({
    run: id => this.api.deleteAssignment(id, { suppressShellFeedback: true }),
    source: 'admin.limits.number',
    onSuccess: () => {
      this.actionNotice.set('admin.limits.child.noticeDeleted');
      this.dataResource.reload();
    },
  });

  readonly allRows = computed<RuleRow[]>(() => {
    const data = this.dataResource.value();
    if (!data) return [];
    const blockingRules = data.specs.filter(rule =>
      (NUMBER_RULE_KEYS as readonly string[]).includes(rule.ruleKey),
    );
    const assignMap = new Map<RuleKey, LimitAssignmentItem>(
      data.assignments.map(assignment => [assignment.ruleKey, assignment]),
    );
    return blockingRules.map(spec => ({ spec, assignment: assignMap.get(spec.ruleKey) ?? null }));
  });

  readonly activeRows = computed(() => this.allRows().filter(r => r.assignment !== null));
  readonly quickActions = NUMBER_LIMIT_ACTIONS;
  private readonly initialQuickActionHandled = signal(false);

  constructor() {
    this.route.queryParamMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(params => {
        if (this.initialQuickActionHandled()) return;
        if (params.get('quick') !== 'block-number') return;
        this.initialQuickActionHandled.set(true);
        queueMicrotask(() => this.openBlockNumberQuick(true));
      });
  }

  openBlockNumberQuick(clearQuickParam = false): void {
    const ref = this.dialog.open(BlockNumberQuickDialogComponent, {
      width: '480px',
      maxWidth: 'calc(100vw - 2rem)',
      data: {},
    });
    ref.afterClosed()
      .pipe(filter(Boolean), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.actionNotice.set('admin.limits.child.noticeSaved');
        this.dataResource.reload();
      });
    if (clearQuickParam) {
      void this.router.navigate([], {
        relativeTo: this.route,
        queryParams: { quick: null },
        queryParamsHandling: 'merge',
        replaceUrl: true,
      });
    }
  }

  openNumberAction(ruleKey: RuleKey): void {
    const row = this.rowFor(ruleKey);
    if (!row) return;
    this.openUpsert(row);
  }

  openQuickAction(ruleKey: RuleKey): void {
    if (ruleKey === 'BLOCK_SELECTION_PER_DRAW') {
      this.openBlockNumberQuick();
      return;
    }
    this.openNumberAction(ruleKey);
  }

  openUpsert(row: RuleRow): void {
    const ref = this.dialog.open(UpsertLimitDialogComponent, { width: '560px', maxWidth: '100vw' });
    ref.componentInstance.init(row.spec, 'TENANT', null, row.assignment);
    ref.afterClosed()
      .pipe(filter(Boolean), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.actionNotice.set('admin.limits.child.noticeSaved');
        this.dataResource.reload();
      });
  }

  confirmDelete(row: RuleRow): void {
    const assignment = row.assignment;
    if (!assignment) return;
    const label = row.spec.label || row.spec.ruleKey;
    const ref = this.dialog.open(TchConfirmDialog, {
      data: {
        title: this.translate.instant('admin.limits.number.confirmDelete.title'),
        message: this.translate.instant('admin.limits.number.confirmDelete.message', { rule: label }),
        confirmLabel: this.translate.instant('common.delete'),
        destructive: true,
      },
    });
    ref.afterClosed()
      .pipe(filter(result => result?.confirmed === true), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.actionNotice.set(null);
        this.deleteAssignment.execute(assignment.id.value);
      });
  }

  isActionAvailable(ruleKey: RuleKey): boolean {
    return this.rowFor(ruleKey) !== null;
  }

  actionStateLabel(ruleKey: RuleKey): string {
    return this.rowFor(ruleKey)?.assignment
      ? this.translate.instant('admin.limits.table.configured')
      : this.translate.instant('admin.limits.table.notConfigured');
  }

  isActionConfigured(ruleKey: RuleKey): boolean {
    const row = this.rowFor(ruleKey);
    return row?.assignment !== null && row?.assignment !== undefined;
  }

  private rowFor(ruleKey: RuleKey): RuleRow | null {
    return this.allRows().find(row => row.spec.ruleKey === ruleKey) ?? null;
  }
}
