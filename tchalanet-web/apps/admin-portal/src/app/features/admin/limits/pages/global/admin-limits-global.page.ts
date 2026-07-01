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
import { TranslateService } from '@ngx-translate/core';
import { forkJoin } from 'rxjs';

import { webAppErrorFromProblemDetail } from '@tch/api';
import type { ProblemDetail } from '@tch/api';
import { TchErrorPanel, TchLoading, TchSectionError } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { AdminEmptyStateComponent } from '@tch/ui/console';

import { AdminLimitsApi } from '../../data-access/admin-limits-api.service';
import type { LimitRuleSpec, RuleRow } from '../../data-access/admin-limits.models';
import { LimitAssignmentsTableComponent } from '../../components/limit-assignments-table/limit-assignments-table.component';
import { UpsertLimitDialogComponent } from '../../components/upsert-limit-dialog/upsert-limit-dialog.component';

@Component({
  selector: 'tch-admin-limits-global-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatButtonModule,
    TchErrorPanel,
    TchLoading,
    TchSectionError,
    AdminEmptyStateComponent,
    LimitAssignmentsTableComponent,
  ],
  templateUrl: './admin-limits-global.page.html',
  styleUrl: './admin-limits-global.page.scss',
})
export class AdminLimitsGlobalPage implements OnInit {
  private readonly api = inject(AdminLimitsApi);
  private readonly dialog = inject(MatDialog);
  private readonly translate = inject(TranslateService);

  readonly loading = signal(false);
  readonly pageError = signal<ErrorViewModel | null>(null);
  readonly actionError = signal<ErrorViewModel | null>(null);
  readonly actionNotice = signal<string | null>(null);
  readonly allRows = signal<RuleRow[]>([]);
  readonly activeRows = computed(() => this.allRows().filter(r => r.assignment !== null));
  readonly unassignedRules = computed<LimitRuleSpec[]>(() => this.allRows().filter(r => !r.assignment).map(r => r.spec));

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
        const assignMap = new Map(view.items.map(a => [a.ruleKey, a]));
        this.allRows.set(rules.map(spec => ({ spec, assignment: assignMap.get(spec.ruleKey) ?? null })));
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.pageError.set(this.resolveError(err, 'admin.limits.global', 'page'));
        this.loading.set(false);
      },
    });
  }

  openAdd(): void {
    const ref = this.dialog.open(UpsertLimitDialogComponent, { width: '560px' });
    ref.componentInstance.initAdd(this.unassignedRules(), 'TENANT', null);
    ref.afterClosed().subscribe((result: unknown) => {
      if (result) {
        this.actionNotice.set('Règle ajoutée.');
        this.reloadAssignments();
      }
    });
  }

  openUpsert(row: RuleRow): void {
    const ref = this.dialog.open(UpsertLimitDialogComponent, { width: '560px' });
    ref.componentInstance.init(row.spec, 'TENANT', null, row.assignment);
    ref.afterClosed().subscribe((result: unknown) => {
      if (result) {
        this.actionNotice.set('Règle enregistrée.');
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
        this.actionNotice.set('Règle supprimée.');
        this.reloadAssignments();
      },
      error: (err: unknown) => {
        this.actionError.set(this.resolveError(err, 'admin.limits.global.delete', 'section'));
      },
    });
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
        this.actionError.set(this.resolveError(err, 'admin.limits.global.reload', 'section'));
      },
    });
  }

  private resolveError(err: unknown, source: string, surface: 'page' | 'section'): ErrorViewModel {
    const problem = (err as { error?: ProblemDetail })?.error;
    if (!problem) {
      return {
        severity: 'error',
        title: this.translate.instant('common.errors.fallback.title'),
        message: this.translate.instant('common.errors.fallback.message'),
      };
    }
    const normalized = webAppErrorFromProblemDetail(problem, source, surface);
    const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
    return toErrorViewModel(normalized, copy);
  }
}
