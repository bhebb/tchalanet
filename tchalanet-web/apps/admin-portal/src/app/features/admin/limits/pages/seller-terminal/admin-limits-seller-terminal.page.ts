import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  signal,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { RouterLink } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { Observable, forkJoin, map } from 'rxjs';

import { webAppErrorFromProblemDetail } from '@tch/api';
import type { ProblemDetail } from '@tch/api';
import { TchErrorPanel, TchLoading, TchSectionError, TchSearchSelect } from '@tch/ui/components';
import type { TchSearchOption } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { AdminEmptyStateComponent } from '@tch/ui/console';

import { SellerTerminalApi } from '../../../seller-terminal-api.service';
import { AdminLimitsApi } from '../../data-access/admin-limits-api.service';
import type { LimitRuleSpec, RuleRow } from '../../data-access/admin-limits.models';
import { LimitAssignmentsTableComponent } from '../../components/limit-assignments-table/limit-assignments-table.component';
import { UpsertLimitDialogComponent } from '../../components/upsert-limit-dialog/upsert-limit-dialog.component';

@Component({
  selector: 'tch-admin-limits-seller-terminal-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatButtonModule,
    RouterLink,
    TchErrorPanel,
    TchLoading,
    TchSectionError,
    TchSearchSelect,
    AdminEmptyStateComponent,
    LimitAssignmentsTableComponent,
  ],
  templateUrl: './admin-limits-seller-terminal.page.html',
  styleUrl: './admin-limits-seller-terminal.page.scss',
})
export class AdminLimitsSellerTerminalPage {
  private readonly api = inject(AdminLimitsApi);
  private readonly sellerTerminalApi = inject(SellerTerminalApi);
  private readonly dialog = inject(MatDialog);
  private readonly translate = inject(TranslateService);

  readonly loading = signal(false);
  readonly pageError = signal<ErrorViewModel | null>(null);
  readonly actionError = signal<ErrorViewModel | null>(null);
  readonly actionNotice = signal<string | null>(null);
  readonly allRows = signal<RuleRow[]>([]);
  readonly activeRows = computed(() => this.allRows().filter(r => r.assignment !== null));
  readonly unassignedRules = computed<LimitRuleSpec[]>(() => this.allRows().filter(r => !r.assignment).map(r => r.spec));
  readonly loadedTerminalCode = signal<string | null>(null);

  readonly searchTerminals = (query: string): Observable<readonly TchSearchOption[]> =>
    this.sellerTerminalApi.list({ q: query, size: 10 }).pipe(
      map(page => page.items.map(row => ({
        id: row.terminalCode,
        title: row.displayName,
        subtitle: row.terminalCode,
      }))),
    );

  onTerminalSelected(option: TchSearchOption | null): void {
    const code = option?.id ?? null;
    this.loadedTerminalCode.set(code);
    this.allRows.set([]);
    this.pageError.set(null);
    this.actionError.set(null);
    this.actionNotice.set(null);
    if (code) this.loadForTerminal(code);
  }

  openAdd(): void {
    const code = this.loadedTerminalCode();
    if (!code) return;
    const ref = this.dialog.open(UpsertLimitDialogComponent, { width: '560px', maxWidth: '100vw' });
    ref.componentInstance.initAdd(this.unassignedRules(), 'TERMINAL', code);
    ref.afterClosed().subscribe((result: unknown) => {
      if (result) {
        this.actionNotice.set('Règle ajoutée.');
        this.reloadAssignments(code);
      }
    });
  }

  openUpsert(row: RuleRow): void {
    const code = this.loadedTerminalCode();
    if (!code) return;
    const ref = this.dialog.open(UpsertLimitDialogComponent, { width: '560px', maxWidth: '100vw' });
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
        this.actionError.set(this.resolveError(err, 'admin.limits.seller-terminal.delete', 'section'));
      },
    });
  }

  private loadForTerminal(code: string): void {
    this.loading.set(true);
    this.pageError.set(null);
    forkJoin([
      this.api.listRules({ suppressShellFeedback: true }),
      this.api.listAssignments('TERMINAL', code, { suppressShellFeedback: true }),
    ]).subscribe({
      next: ([rules, view]) => {
        const assignMap = new Map(view.items.map(a => [a.ruleKey, a]));
        this.allRows.set(rules.map(spec => ({ spec, assignment: assignMap.get(spec.ruleKey) ?? null })));
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.pageError.set(this.resolveError(err, 'admin.limits.seller-terminal', 'page'));
        this.loading.set(false);
      },
    });
  }

  private reloadAssignments(code: string): void {
    this.api.listAssignments('TERMINAL', code, { suppressShellFeedback: true }).subscribe({
      next: view => {
        const assignMap = new Map(view.items.map(a => [a.ruleKey, a]));
        this.allRows.update(current =>
          current.map(r => ({ spec: r.spec, assignment: assignMap.get(r.spec.ruleKey) ?? null })),
        );
      },
      error: (err: unknown) => {
        this.actionError.set(this.resolveError(err, 'admin.limits.seller-terminal.reload', 'section'));
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
