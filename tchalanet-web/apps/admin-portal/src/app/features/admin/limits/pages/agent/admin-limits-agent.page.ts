import {
  ChangeDetectionStrategy,
  Component,
  inject,
  signal,
} from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { TranslateService } from '@ngx-translate/core';
import { forkJoin } from 'rxjs';

import { webAppErrorFromProblemDetail } from '@tch/api';
import type { ProblemDetail } from '@tch/api';
import { TchErrorPanel, TchLoading, TchSectionError } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { AdminEmptyStateComponent } from '@tch/ui/console';

import { AdminLimitsApi } from '../../data-access/admin-limits-api.service';
import type { RuleRow } from '../../data-access/admin-limits.models';
import { LimitAssignmentsTableComponent } from '../../components/limit-assignments-table/limit-assignments-table.component';
import { UpsertLimitDialogComponent } from '../../components/upsert-limit-dialog/upsert-limit-dialog.component';

@Component({
  selector: 'tch-admin-limits-agent-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    TchErrorPanel,
    TchLoading,
    TchSectionError,
    AdminEmptyStateComponent,
    LimitAssignmentsTableComponent,
  ],
  templateUrl: './admin-limits-agent.page.html',
  styleUrl: './admin-limits-agent.page.scss',
})
export class AdminLimitsAgentPage {
  private readonly api = inject(AdminLimitsApi);
  private readonly dialog = inject(MatDialog);
  private readonly translate = inject(TranslateService);

  readonly terminalCtrl = new FormControl('', { nonNullable: true, validators: [Validators.required] });

  readonly loading = signal(false);
  readonly pageError = signal<ErrorViewModel | null>(null);
  readonly actionError = signal<ErrorViewModel | null>(null);
  readonly actionNotice = signal<string | null>(null);
  readonly rows = signal<RuleRow[]>([]);
  readonly loadedTerminalCode = signal<string | null>(null);

  loadForTerminal(): void {
    const code = this.terminalCtrl.value.trim();
    if (!code) return;
    this.loadedTerminalCode.set(code);
    this.loading.set(true);
    this.pageError.set(null);
    this.actionError.set(null);
    this.actionNotice.set(null);
    forkJoin([
      this.api.listRules({ suppressShellFeedback: true }),
      this.api.listAssignments('TERMINAL', code, { suppressShellFeedback: true }),
    ]).subscribe({
      next: ([rules, view]) => {
        const assignMap = new Map(view.items.map(a => [a.ruleKey, a]));
        this.rows.set(rules.map(spec => ({ spec, assignment: assignMap.get(spec.ruleKey) ?? null })));
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.pageError.set(this.resolveError(err, 'admin.limits.agent', 'page'));
        this.loading.set(false);
      },
    });
  }

  openUpsert(row: RuleRow): void {
    const code = this.loadedTerminalCode();
    if (!code) return;
    const ref = this.dialog.open(UpsertLimitDialogComponent, { width: '560px' });
    ref.componentInstance.init(row.spec, 'TERMINAL', code, row.assignment);
    ref.afterClosed().subscribe((result: unknown) => {
      if (result) {
        this.actionNotice.set('Règle enregistrée.');
        this.reloadAssignments(code);
      }
    });
  }

  confirmDelete(row: RuleRow): void {
    if (!row.assignment) return;
    const code = this.loadedTerminalCode();
    if (!code) return;
    if (!confirm(`Supprimer la règle « ${row.spec.label || row.spec.ruleKey} » ?`)) return;
    this.actionError.set(null);
    this.actionNotice.set(null);
    this.api.deleteAssignment(row.assignment.id.value, { suppressShellFeedback: true }).subscribe({
      next: () => {
        this.actionNotice.set('Règle supprimée.');
        this.reloadAssignments(code);
      },
      error: (err: unknown) => {
        this.actionError.set(this.resolveError(err, 'admin.limits.agent.delete', 'section'));
      },
    });
  }

  private reloadAssignments(code: string): void {
    this.api.listAssignments('TERMINAL', code, { suppressShellFeedback: true }).subscribe({
      next: view => {
        const assignMap = new Map(view.items.map(a => [a.ruleKey, a]));
        this.rows.update(current =>
          current.map(r => ({ spec: r.spec, assignment: assignMap.get(r.spec.ruleKey) ?? null })),
        );
      },
      error: (err: unknown) => {
        this.actionError.set(this.resolveError(err, 'admin.limits.agent.reload', 'section'));
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
