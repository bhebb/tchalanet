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

import { mapHttpErrorToProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import {
  TchCard,
  TchConfirmDialog,
  TchErrorPanel,
  TchLoading,
  TchSectionError,
} from '@tch/ui/components';
import { AdminDetailLayoutComponent, AdminSectionCardComponent } from '@tch/ui/console';
import { ErrorViewModel, resolveErrorFeedbackCopy, toErrorViewModel } from '@tch/web/errors';

import { AdminLimitsApi } from '../../data-access/admin-limits-api.service';
import {
  ActiveLimitGroup,
  ActiveLimitItem,
  TenantAdminPoliciesOverviewView,
  formatActiveLimitParams,
} from '../../data-access/admin-limits.models';
import { BlockNumberQuickDialogComponent } from '../../components/block-number-quick-dialog/block-number-quick-dialog.component';

interface ActiveLimitGroupVm {
  readonly id: ActiveLimitGroup;
  readonly items: ActiveLimitItem[];
}

const GROUP_ORDER: ActiveLimitGroup[] = [
  'NUMBER_BLOCK',
  'NUMBER_CAP',
  'TICKET_LIMIT',
  'SELLER_LIMIT',
  'ADVANCED',
];

@Component({
  selector: 'tch-admin-limits-overview-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslatePipe,
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

  readonly warningCount = computed(() => this.overview()?.summary.warnings ?? 0);
  readonly activeLimits = computed(() => this.overview()?.activeLimits ?? []);
  readonly activeLimitGroups = computed<ActiveLimitGroupVm[]>(() =>
    GROUP_ORDER.map(id => ({
      id,
      items: this.activeLimits().filter(item => item.group === id),
    })).filter(group => group.items.length > 0),
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

  disableLimit(item: ActiveLimitItem): void {
    this.dialog
      .open(TchConfirmDialog, {
        data: {
          title: this.translate.instant('admin.limits.overview.confirmDisable'),
          message: this.confirmMessage(item),
          confirmLabel: this.translate.instant('admin.limits.overview.actions.disable'),
          cancelLabel: this.translate.instant('common.cancel'),
          icon: 'pause_circle',
        },
      })
      .afterClosed()
      .subscribe(result => {
        if (result?.confirmed !== true) return;
        this.actionError.set(null);
        this.actionNotice.set(null);
        this.api
          .upsertAssignment(
            {
              ruleKey: item.ruleKey,
              targetType: item.targetType,
              targetId: item.targetType === 'TENANT' ? null : item.targetId,
              enabled: false,
              onBreach: item.onBreach,
              params: item.params,
              startsAt: item.startsAt,
              endsAt: item.endsAt,
            },
            { suppressShellFeedback: true },
          )
          .subscribe({
            next: () => {
              this.actionNotice.set('admin.limits.common.notice.saved');
              this.reloadOverview();
            },
            error: (err: unknown) => {
              this.actionError.set(this.resolveError(err, 'section'));
            },
          });
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
        this.actionError.set(null);
        this.actionNotice.set(null);
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

  numberRuleCount(): number {
    return this.overview()?.summary.numberRules ?? 0;
  }

  groupTitleKey(group: ActiveLimitGroup): string {
    return `admin.limits.overview.groups.${group}.title`;
  }

  groupDescriptionKey(group: ActiveLimitGroup): string {
    return `admin.limits.overview.groups.${group}.description`;
  }

  ruleLabelKey(item: ActiveLimitItem): string {
    return `admin.limits.rule.${item.ruleKey}`;
  }

  statusKey(item: ActiveLimitItem): string {
    return item.enabled
      ? 'admin.limits.overview.limitStatus.active'
      : 'admin.limits.overview.limitStatus.disabled';
  }

  targetLabel(item: ActiveLimitItem): string {
    if (item.targetType === 'TENANT') {
      return this.translate.instant('admin.limits.overview.target.tenant');
    }
    return item.targetLabel || item.targetCode || item.targetId;
  }

  durationLabel(item: ActiveLimitItem): string {
    if (!item.startsAt && !item.endsAt) {
      return this.translate.instant('admin.limits.overview.duration.permanent');
    }
    if (item.endsAt) {
      return this.translate.instant('admin.limits.overview.duration.until', {
        date: new Date(item.endsAt).toLocaleDateString(),
      });
    }
    return this.translate.instant('admin.limits.overview.duration.custom');
  }

  paramsLabel(item: ActiveLimitItem): string {
    return formatActiveLimitParams(item);
  }

  private confirmMessage(item: ActiveLimitItem): string {
    return [
      this.translate.instant(this.ruleLabelKey(item)),
      this.targetLabel(item),
      this.paramsLabel(item),
    ].filter(Boolean).join(' · ');
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
