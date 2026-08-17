import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { forkJoin } from 'rxjs';

import { mapHttpErrorToProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { TchConfirmDialog, TchErrorPanel, TchLoading, TchSectionError } from '@tch/ui/components';
import {
  AdminDetailLayoutComponent,
  AdminPageShellComponent,
  AdminSectionCardComponent,
  TchIdentityCardComponent,
} from '@tch/ui/console';
import { ErrorViewModel, resolveErrorFeedbackCopy, toErrorViewModel } from '@tch/web/errors';

import { AdminLimitsApi } from '../../data-access/admin-limits-api.service';
import type {
  ActiveLimitItem,
  LimitAssignmentItem,
  LimitRuleSpec,
} from '../../data-access/admin-limits.models';
import { formatActiveLimitParams } from '../../data-access/admin-limits.models';
import { UpsertLimitDialogComponent } from '../../components/upsert-limit-dialog/upsert-limit-dialog.component';
import { MatDialog } from '@angular/material/dialog';

@Component({
  selector: 'tch-admin-limits-detail-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslatePipe,
    RouterLink,
    MatButtonModule,
    MatMenuModule,
    AdminDetailLayoutComponent,
    AdminPageShellComponent,
    AdminSectionCardComponent,
    TchIdentityCardComponent,
    TchErrorPanel,
    TchLoading,
    TchSectionError,
  ],
  templateUrl: './admin-limits-detail.page.html',
  styleUrl: './admin-limits-detail.page.scss',
})
export class AdminLimitsDetailPage implements OnInit {
  private readonly api = inject(AdminLimitsApi);
  private readonly dialog = inject(MatDialog);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly translate = inject(TranslateService);

  readonly loading = signal(false);
  readonly pageError = signal<ErrorViewModel | null>(null);
  readonly actionError = signal<ErrorViewModel | null>(null);
  readonly actionNotice = signal<string | null>(null);
  readonly item = signal<ActiveLimitItem | null>(null);
  readonly spec = signal<LimitRuleSpec | null>(null);

  private get assignmentId(): string {
    return this.route.snapshot.paramMap.get('assignmentId') ?? '';
  }

  readonly ruleLabel = computed(() => {
    const s = this.spec();
    return s ? this.translate.instant(`admin.limits.rule.${s.ruleKey}.label`) : '';
  });

  readonly ruleDescription = computed(() => {
    const s = this.spec();
    if (!s) return '';
    const key = `admin.limits.rule.${s.ruleKey}.description`;
    const translated = this.translate.instant(key);
    return translated !== key ? translated : (s.description ?? '');
  });

  readonly statusTone = computed(() =>
    this.item()?.enabled ? ('success' as const) : ('warning' as const),
  );

  readonly statusLabel = computed(() =>
    this.item()?.enabled
      ? this.translate.instant('admin.limits.overview.limitStatus.active')
      : this.translate.instant('admin.limits.overview.limitStatus.disabled'),
  );

  readonly scopeLabel = computed(() => {
    const t = this.item()?.targetType;
    if (!t) return '';
    return this.translate.instant(`admin.limits.overview.scopeType.${t}`);
  });

  readonly targetLabel = computed(() => {
    const i = this.item();
    if (!i) return '';
    if (i.targetType === 'TENANT') {
      return this.translate.instant('admin.limits.overview.target.tenant');
    }
    return i.targetLabel || i.targetCode || i.targetId;
  });

  readonly paramsLabel = computed(() => {
    const i = this.item();
    return i ? formatActiveLimitParams(i) : '—';
  });

  readonly outcomeLabel = computed(() => {
    const outcome = this.item()?.onBreach;
    if (outcome === 'BLOCK') return this.translate.instant('admin.limits.dialog.block');
    if (outcome === 'WARN') return this.translate.instant('admin.limits.dialog.warn');
    return outcome ?? '—';
  });

  readonly validityLabel = computed(() => {
    const i = this.item();
    if (!i) return '—';
    if (!i.startsAt && !i.endsAt)
      return this.translate.instant('admin.limits.overview.duration.permanent');
    if (i.endsAt)
      return this.translate.instant('admin.limits.overview.duration.until', {
        date: new Date(i.endsAt).toLocaleDateString(),
      });
    return this.translate.instant('admin.limits.overview.duration.custom');
  });

  readonly ruleKeyCode = computed(() => this.spec()?.ruleKey ?? '');

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.pageError.set(null);

    const opts = { suppressShellFeedback: true };
    forkJoin([
      this.api.overview(opts),
      this.api.listRules(opts),
    ]).subscribe({
      next: ([overview, specs]) => {
        const found = (overview.activeLimits ?? []).find(
          l => l.assignmentId === this.assignmentId,
        );
        if (!found) {
          this.pageError.set({
            title: this.translate.instant('common.notFound'),
            message: this.translate.instant('admin.limits.detail.notFound'),
            severity: 'error',
          } as ErrorViewModel);
        } else {
          this.item.set(found);
          this.spec.set(specs.find(s => s.ruleKey === found.ruleKey) ?? null);
        }
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.pageError.set(this.resolveError(err));
        this.loading.set(false);
      },
    });
  }

  editLimit(): void {
    const item = this.item();
    const spec = this.spec();
    if (!item || !spec) return;

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
        this.load();
      }
    });
  }

  disableLimit(): void {
    const item = this.item();
    if (!item) return;

    this.dialog
      .open(TchConfirmDialog, {
        data: {
          title: this.translate.instant('admin.limits.overview.confirmDisable'),
          message: this.ruleLabel(),
          confirmLabel: this.translate.instant('admin.limits.overview.actions.disable'),
          cancelLabel: this.translate.instant('common.cancel'),
          icon: 'pause_circle',
        },
      })
      .afterClosed()
      .subscribe(result => {
        if (result?.confirmed !== true) return;
        this.clearFeedback();
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
              this.load();
            },
            error: (err: unknown) => {
              this.actionError.set(this.resolveError(err, 'section'));
            },
          });
      });
  }

  deleteLimit(): void {
    const item = this.item();
    if (!item) return;

    this.dialog
      .open(TchConfirmDialog, {
        data: {
          title: this.translate.instant('admin.limits.overview.confirmDelete'),
          message: this.ruleLabel(),
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
            this.router.navigate(['/app/admin/limits']);
          },
          error: (err: unknown) => {
            this.actionError.set(this.resolveError(err, 'section'));
          },
        });
      });
  }

  private clearFeedback(): void {
    this.actionError.set(null);
    this.actionNotice.set(null);
  }

  private resolveError(err: unknown, surface: 'page' | 'section' = 'page'): ErrorViewModel {
    const problem = mapHttpErrorToProblemDetail(err);
    const normalized = webAppErrorFromProblemDetail(problem, 'admin.limits.detail', surface);
    const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
    return toErrorViewModel(normalized, copy);
  }
}
