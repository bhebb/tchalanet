import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
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
import type { LimitAssignmentItem, LimitRuleSpec } from '../../data-access/admin-limits.models';
import { detectParamSchema, extractParamValues } from '../../data-access/admin-limits.models';
import { BlockNumberQuickDialogComponent } from '../../components/block-number-quick-dialog/block-number-quick-dialog.component';

export interface BlockedNumberRow {
  id: { value: string };
  channelLabel: string | null;
  numbers: string[];
  endsAt: string | null;
}

@Component({
  selector: 'tch-admin-limits-number-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    TranslatePipe,
    MatButtonModule,
    RouterLink,
    TchErrorPanel,
    TchLoading,
    TchSectionError,
    AdminEmptyStateComponent,
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
  readonly blockSpec = signal<LimitRuleSpec | null>(null);
  readonly blockedRows = signal<BlockedNumberRow[]>([]);

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
      this.api.listAssignments('DRAW_CHANNEL', undefined, { suppressShellFeedback: true }),
    ]).subscribe({
      next: ([rules, channelView]) => {
        const spec = rules.find(r => r.ruleKey === 'BLOCK_SELECTION_PER_DRAW') ?? null;
        this.blockSpec.set(spec);
        this.blockedRows.set(
          channelView.items
            .filter(a => a.ruleKey === 'BLOCK_SELECTION_PER_DRAW' && a.enabled)
            .map(a => this.toBlockedRow(a, spec)),
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
    const ref = this.dialog.open(BlockNumberQuickDialogComponent, {
      width: '480px',
      maxWidth: 'calc(100vw - 2rem)',
    });
    ref.componentInstance.spec = this.blockSpec();
    ref.afterClosed().subscribe((result: unknown) => {
      if (result) {
        this.actionNotice.set('admin.limits.blockNumber.noticeSaved');
        this.reloadBlocked();
      }
    });
  }

  deleteBlocked(row: BlockedNumberRow): void {
    if (!confirm(this.translate.instant('admin.limits.blockNumber.deleteConfirm'))) return;
    this.actionError.set(null);
    this.actionNotice.set(null);
    this.api.deleteAssignment(row.id.value, { suppressShellFeedback: true }).subscribe({
      next: () => {
        this.actionNotice.set('admin.limits.blockNumber.noticeDeleted');
        this.reloadBlocked();
      },
      error: (err: unknown) => {
        this.actionError.set(this.resolveError(err, 'admin.limits.number.delete', 'section'));
      },
    });
  }

  expiryLabel(row: BlockedNumberRow): string {
    if (!row.endsAt) return this.translate.instant('admin.limits.blockNumber.durationPermanent');
    const d = new Date(row.endsAt);
    const today = new Date();
    const isToday =
      d.getFullYear() === today.getFullYear() &&
      d.getMonth() === today.getMonth() &&
      d.getDate() === today.getDate();
    return isToday
      ? this.translate.instant('admin.limits.blockNumber.durationToday')
      : d.toLocaleDateString();
  }

  private toBlockedRow(a: LimitAssignmentItem, spec: LimitRuleSpec | null): BlockedNumberRow {
    const schema = spec ? detectParamSchema(spec) : 'SELECTION';
    const values = extractParamValues(schema, spec?.paramsTemplate ?? {}, a.params);
    return {
      id: a.id,
      channelLabel: a.targetLabel ?? a.targetId ?? null,
      numbers: values.selectionIds,
      endsAt: a.endsAt,
    };
  }

  private reloadBlocked(): void {
    this.api
      .listAssignments('DRAW_CHANNEL', undefined, { suppressShellFeedback: true })
      .subscribe({
        next: view => {
          const spec = this.blockSpec();
          this.blockedRows.set(
            view.items
              .filter(a => a.ruleKey === 'BLOCK_SELECTION_PER_DRAW' && a.enabled)
              .map(a => this.toBlockedRow(a, spec)),
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
