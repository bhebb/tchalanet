import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { Observable, of, tap } from 'rxjs';

import { mapHttpErrorToProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import {
  TchConfirmDialog,
  TchErrorPanel,
  TchLoading,
  TchSectionError,
} from '@tch/ui/components';
import { ErrorViewModel, resolveErrorFeedbackCopy, toErrorViewModel } from '@tch/web/errors';

import { AdminLimitsApi } from '../../data-access/admin-limits-api.service';
import {
  ActiveLimitItem,
  LimitAssignmentItem,
  LimitRuleSpec,
  TenantAdminPoliciesOverviewView,
  TargetType,
  formatActiveLimitParams,
} from '../../data-access/admin-limits.models';
import { BlockNumberQuickDialogComponent } from '../../components/block-number-quick-dialog/block-number-quick-dialog.component';
import { UpsertLimitDialogComponent } from '../../components/upsert-limit-dialog/upsert-limit-dialog.component';
import { AdminLimitItemCardComponent } from '../../components/limit-item-card/admin-limit-item-card.component';
import { MatDialog } from '@angular/material/dialog';

type ScopeFilter = 'ALL' | TargetType;

@Component({
  selector: 'tch-admin-limits-overview-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslatePipe,
    MatButtonModule,
    MatButtonToggleModule,
    TchErrorPanel,
    TchLoading,
    TchSectionError,
    AdminLimitItemCardComponent,
  ],
  templateUrl: './admin-limits-overview.page.html',
  styleUrl: './admin-limits-overview.page.scss',
})
export class AdminLimitsOverviewPage implements OnInit {
  private readonly api = inject(AdminLimitsApi);
  private readonly dialog = inject(MatDialog);
  private readonly translate = inject(TranslateService);

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly pageError = signal<ErrorViewModel | null>(null);
  readonly actionError = signal<ErrorViewModel | null>(null);
  readonly actionNotice = signal<string | null>(null);
  readonly overview = signal<TenantAdminPoliciesOverviewView | null>(null);
  readonly scopeFilter = signal<ScopeFilter>('ALL');

  private readonly rulesCache = signal<LimitRuleSpec[]>([]);

  readonly activeLimits = computed(() => this.overview()?.activeLimits ?? []);

  readonly activeCount = computed(() => this.activeLimits().filter(l => l.enabled).length);
  readonly blockCount = computed(
    () => this.activeLimits().filter(l => l.enabled && l.group === 'NUMBER_BLOCK').length,
  );
  readonly capCount = computed(
    () => this.activeLimits().filter(l => l.enabled && l.group === 'NUMBER_CAP').length,
  );

  readonly inlineStats = computed(() => {
    const total = this.activeCount();
    if (total === 0) return null;
    const blocks = this.blockCount();
    const caps = this.capCount();
    const parts: string[] = [`${total} limit aktif`];
    if (blocks > 0) parts.push(`${blocks} blokaj nimewo`);
    if (caps > 0) parts.push(`${caps} limit miz`);
    return parts.join(' · ');
  });

  readonly filteredLimits = computed(() => {
    const filter = this.scopeFilter();
    if (filter === 'ALL') return this.activeLimits();
    return this.activeLimits().filter(l => l.targetType === filter);
  });

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

  openBlockNumber(): void {
    const ref = this.dialog.open(BlockNumberQuickDialogComponent, {
      width: '560px',
      maxWidth: '100vw',
    });
    ref.afterClosed().subscribe((result: unknown) => {
      if (result) {
        this.actionNotice.set('admin.limits.common.notice.saved');
        this.reloadOverview();
      }
    });
  }

  openPlafon(): void {
    this.withSpec('MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW', spec => {
      const ref = this.dialog.open(UpsertLimitDialogComponent, {
        width: '560px',
        maxWidth: '100vw',
      });
      ref.componentInstance.init(spec, 'TENANT', null, null);
      ref.afterClosed().subscribe((result: unknown) => {
        if (result) {
          this.actionNotice.set('admin.limits.common.notice.saved');
          this.reloadOverview();
        }
      });
    });
  }

  editLimit(item: ActiveLimitItem): void {
    this.withSpec(item.ruleKey, spec => {
      const assignment: LimitAssignmentItem = {
        id: { value: item.assignmentId },
        ruleKey: item.ruleKey,
        enabled: item.enabled,
        onBreach: item.onBreach,
        params: item.params,
        startsAt: item.startsAt,
        endsAt: item.endsAt,
      };
      const ref = this.dialog.open(UpsertLimitDialogComponent, {
        width: '560px',
        maxWidth: '100vw',
      });
      ref.componentInstance.init(
        spec,
        item.targetType,
        item.targetType === 'TENANT' ? null : item.targetId,
        assignment,
      );
      ref.afterClosed().subscribe((result: unknown) => {
        if (result) {
          this.actionNotice.set('admin.limits.common.notice.saved');
          this.reloadOverview();
        }
      });
    });
  }

  toggleLimit(item: ActiveLimitItem): void {
    this.clearFeedback();
    this.saving.set(true);
    this.api
      .upsertAssignment(
        {
          ruleKey: item.ruleKey,
          targetType: item.targetType,
          targetId: item.targetType === 'TENANT' ? null : item.targetId,
          enabled: !item.enabled,
          onBreach: item.onBreach,
          params: item.params,
          startsAt: item.startsAt,
          endsAt: item.endsAt,
        },
        { suppressShellFeedback: true },
      )
      .subscribe({
        next: () => {
          this.saving.set(false);
          this.actionNotice.set('admin.limits.common.notice.saved');
          this.reloadOverview();
        },
        error: (err: unknown) => {
          this.saving.set(false);
          this.actionError.set(this.resolveError(err, 'section'));
        },
      });
  }

  deleteLimit(item: ActiveLimitItem): void {
    this.dialog
      .open(TchConfirmDialog, {
        data: {
          title: this.translate.instant('admin.limits.overview.confirmDelete'),
          message: this.confirmMessage(item),
          confirmLabel: this.translate.instant('common.delete'),
          cancelLabel: this.translate.instant('common.cancel'),
          destructive: true,
          icon: 'delete',
        },
      })
      .afterClosed()
      .subscribe(result => {
        if (result?.confirmed !== true) return;
        this.clearFeedback();
        this.api.deleteAssignment(item.assignmentId, { suppressShellFeedback: true }).subscribe({
          next: () => {
            this.actionNotice.set('admin.limits.common.notice.deleted');
            this.reloadOverview();
          },
          error: (err: unknown) => {
            this.actionError.set(this.resolveError(err, 'section'));
          },
        });
      });
  }

  private withSpec(ruleKey: string, fn: (spec: LimitRuleSpec) => void): void {
    this.loadRules().subscribe(specs => {
      const spec = specs.find(s => s.ruleKey === ruleKey);
      if (spec) fn(spec);
    });
  }

  private loadRules(): Observable<LimitRuleSpec[]> {
    const cached = this.rulesCache();
    if (cached.length) return of(cached);
    return this.api.listRules({ suppressShellFeedback: true }).pipe(
      tap(specs => this.rulesCache.set(specs)),
    );
  }

  private confirmMessage(item: ActiveLimitItem): string {
    const ruleLabel = this.translate.instant(`admin.limits.rule.${item.ruleKey}.label`);
    const targetLabel =
      item.targetType === 'TENANT'
        ? this.translate.instant('admin.limits.overview.target.tenant')
        : (item.targetLabel ?? item.targetCode ?? item.targetId);
    return [ruleLabel, targetLabel, formatActiveLimitParams(item)].filter(Boolean).join(' · ');
  }

  private clearFeedback(): void {
    this.actionError.set(null);
    this.actionNotice.set(null);
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
    const problem = mapHttpErrorToProblemDetail(err);
    const normalized = webAppErrorFromProblemDetail(problem, 'admin.limits.overview', surface);
    const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
    return toErrorViewModel(normalized, copy);
  }
}
