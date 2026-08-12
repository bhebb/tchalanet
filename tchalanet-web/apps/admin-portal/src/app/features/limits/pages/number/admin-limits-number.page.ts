import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { RouterLink } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { forkJoin } from 'rxjs';

import { mapHttpErrorToProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { TchErrorPanel, TchLoading, TchSectionError } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { AdminEmptyStateComponent } from '@tch/ui/console';

import { AdminLimitsApi } from '../../data-access/admin-limits-api.service';
import type { RuleKey, RuleRow } from '../../data-access/admin-limits.models';
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
    RouterLink,
    TchErrorPanel,
    TchLoading,
    TchSectionError,
    AdminEmptyStateComponent,
    LimitAssignmentsTableComponent,
  ],
  templateUrl: './admin-limits-number.page.html',
  styleUrl: './admin-limits-number.page.scss',
})
export class AdminLimitsNumberPage implements OnInit {
  private readonly api = inject(AdminLimitsApi);
  private readonly dialog = inject(MatDialog);
  private readonly translate = inject(TranslateService);

  readonly loading = signal(false);
  readonly pageError = signal<ErrorViewModel | null>(null);
  readonly actionError = signal<ErrorViewModel | null>(null);
  readonly actionNotice = signal<string | null>(null);
  readonly allRows = signal<RuleRow[]>([]);
  readonly activeRows = computed(() => this.allRows().filter(r => r.assignment !== null));
  readonly quickActions = NUMBER_LIMIT_ACTIONS;

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.pageError.set(null);
    this.actionError.set(null);
    this.actionNotice.set(null);
    forkJoin([
      this.api.listRules({ suppressShellFeedback: true }),
      this.api.listAssignments('TENANT', undefined, { suppressShellFeedback: true }),
    ]).subscribe({
      next: ([rules, view]) => {
        const blockingRules = rules.filter(r => (NUMBER_RULE_KEYS as string[]).includes(r.ruleKey));
        const assignMap = new Map(view.items.map(a => [a.ruleKey, a]));
        this.allRows.set(
          blockingRules.map(spec => ({ spec, assignment: assignMap.get(spec.ruleKey) ?? null })),
        );
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.pageError.set(this.resolveError(err, 'admin.limits.number', 'page'));
        this.loading.set(false);
      },
    });
  }

  openBlockNumberQuick(): void {
    const spec = this.rowFor('BLOCK_SELECTION_PER_DRAW')?.spec ?? null;
    const ref = this.dialog.open(BlockNumberQuickDialogComponent, {
      width: '480px',
      maxWidth: 'calc(100vw - 2rem)',
    });
    ref.componentInstance.spec = spec;
    ref.afterClosed().subscribe((result: unknown) => {
      if (result) {
        this.actionNotice.set('admin.limits.child.noticeSaved');
        this.reloadAssignments();
      }
    });
  }

  openNumberAction(ruleKey: RuleKey): void {
    if (ruleKey === 'BLOCK_SELECTION_PER_DRAW') {
      this.openBlockNumberQuick();
      return;
    }
    const row = this.rowFor(ruleKey);
    if (!row) return;
    this.openUpsert(row);
  }

  openUpsert(row: RuleRow): void {
    const ref = this.dialog.open(UpsertLimitDialogComponent, { width: '560px', maxWidth: '100vw' });
    ref.componentInstance.init(row.spec, 'TENANT', null, row.assignment);
    ref.afterClosed().subscribe((result: unknown) => {
      if (result) {
        this.actionNotice.set('admin.limits.child.noticeSaved');
        this.reloadAssignments();
      }
    });
  }

  confirmDelete(row: RuleRow): void {
    if (!row.assignment) return;
    if (!confirm(`Supprimer la règle « ${row.spec.label || row.spec.ruleKey} » ?`)) return;
    this.actionError.set(null);
    this.actionNotice.set(null);
    this.api.deleteAssignment(row.assignment.id.value, { suppressShellFeedback: true }).subscribe({
      next: () => {
        this.actionNotice.set('admin.limits.child.noticeDeleted');
        this.reloadAssignments();
      },
      error: (err: unknown) => {
        this.actionError.set(this.resolveError(err, 'admin.limits.number.delete', 'section'));
      },
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
    return (
      this.rowFor(ruleKey)?.assignment !== null && this.rowFor(ruleKey)?.assignment !== undefined
    );
  }

  private rowFor(ruleKey: RuleKey): RuleRow | null {
    return this.allRows().find(row => row.spec.ruleKey === ruleKey) ?? null;
  }

  private reloadAssignments(): void {
    this.api.listAssignments('TENANT', undefined, { suppressShellFeedback: true }).subscribe({
      next: view => {
        const assignMap = new Map(view.items.map(a => [a.ruleKey, a]));
        this.allRows.update(current =>
          current.map(r => ({ spec: r.spec, assignment: assignMap.get(r.spec.ruleKey) ?? null })),
        );
      },
      error: (err: unknown) => {
        this.actionError.set(this.resolveError(err, 'admin.limits.number.reload', 'section'));
      },
    });
  }

  private resolveError(err: unknown, source: string, surface: 'page' | 'section'): ErrorViewModel {
    const problem = mapHttpErrorToProblemDetail(err);
    const normalized = webAppErrorFromProblemDetail(problem, source, surface);
    const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
    return toErrorViewModel(normalized, copy);
  }
}
