import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { RouterLink } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { TchCard, TchErrorPanel, TchLoading, TchSectionError } from '@tch/ui/components';
import { AdminDetailLayoutComponent, AdminSectionCardComponent } from '@tch/ui/console';
import { ErrorViewModel, resolveErrorFeedbackCopy, toErrorViewModel } from '@tch/web/errors';

import { AdminLimitsApi } from '../../data-access/admin-limits-api.service';
import {
  LimitOverviewActionLink,
  LimitOverviewCard,
  RuleRow,
  TenantAdminPoliciesOverviewView,
  formatLimitSentence,
} from '../../data-access/admin-limits.models';
import { UpsertLimitDialogComponent } from '../../components/upsert-limit-dialog/upsert-limit-dialog.component';

@Component({
  selector: 'tch-admin-limits-overview-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatButtonModule,
    RouterLink,
    AdminDetailLayoutComponent,
    AdminSectionCardComponent,
    TchCard,
    TchErrorPanel,
    TchLoading,
    TchSectionError,
  ],
  templateUrl: './admin-limits-overview.page.html',
  styleUrl: './admin-limits-overview.page.scss',
})
export class AdminLimitsOverviewPage implements OnInit {
  private readonly api = inject(AdminLimitsApi);
  private readonly dialog = inject(MatDialog);
  private readonly translate = inject(TranslateService);

  readonly loading = signal(false);
  readonly pageError = signal<ErrorViewModel | null>(null);
  readonly actionError = signal<ErrorViewModel | null>(null);
  readonly actionNotice = signal<string | null>(null);
  readonly overview = signal<TenantAdminPoliciesOverviewView | null>(null);

  readonly globalRows = computed<RuleRow[]>(() =>
    this.overview()?.globalRules.map(row => ({
      spec: row.spec,
      assignment: row.assignment,
    })) ?? [],
  );
  readonly activeGlobalRows = computed(() => this.globalRows().filter(row => row.assignment?.enabled));
  readonly warningCount = computed(() =>
    this.overview()?.summary.warnings ?? 0,
  );
  readonly activeProtections = computed(() => this.activeGlobalRows());
  readonly cards = computed<readonly LimitOverviewCard[]>(() => this.overview()?.scopeCards ?? []);
  readonly actionLinks = computed<readonly LimitOverviewActionLink[]>(
    () => this.overview()?.actionLinks ?? [],
  );

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.pageError.set(null);
    this.actionError.set(null);
    this.actionNotice.set(null);
    this.api.overview({ suppressShellFeedback: true }).subscribe({
      next: view => {
        this.overview.set(view);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.pageError.set(this.resolveError(err));
        this.loading.set(false);
      },
    });
  }

  ruleSentence(row: RuleRow): string {
    return formatLimitSentence(row);
  }

  openUpsert(row: RuleRow): void {
    const ref = this.dialog.open(UpsertLimitDialogComponent, { width: '560px', maxWidth: '100vw' });
    ref.componentInstance.init(row.spec, 'TENANT', null, row.assignment);
    ref.afterClosed().subscribe((result: unknown) => {
      if (result) {
        this.actionNotice.set('Protection enregistrée.');
        this.reloadOverview();
      }
    });
  }

  confirmDelete(row: RuleRow): void {
    if (!row.assignment) return;
    if (!confirm(`Supprimer la protection « ${row.spec.label || row.spec.ruleKey} » ?`)) return;
    this.actionError.set(null);
    this.actionNotice.set(null);
    this.api.deleteAssignment(row.assignment.id.value, { suppressShellFeedback: true }).subscribe({
      next: () => {
        this.actionNotice.set('Protection supprimée.');
        this.reloadOverview();
      },
      error: (err: unknown) => {
        this.actionError.set(this.resolveError(err, 'section'));
      },
    });
  }

  numberRuleCount(): number {
    return this.overview()?.summary.numberRules ?? 0;
  }

  private reloadOverview(): void {
    this.api.overview({ suppressShellFeedback: true }).subscribe({
      next: view => this.overview.set(view),
      error: (err: unknown) => {
        this.actionError.set(this.resolveError(err, 'section'));
      },
    });
  }

  private resolveError(err: unknown, surface: 'page' | 'section' = 'page'): ErrorViewModel {
    const problem = (err as { error?: ProblemDetail })?.error;
    if (!problem) {
      return {
        severity: 'error',
        title: this.translate.instant('common.errors.fallback.title'),
        message: this.translate.instant('common.errors.fallback.message'),
      };
    }
    const normalized = webAppErrorFromProblemDetail(problem, 'admin.limits.overview', surface);
    const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
    return toErrorViewModel(normalized, copy);
  }
}
