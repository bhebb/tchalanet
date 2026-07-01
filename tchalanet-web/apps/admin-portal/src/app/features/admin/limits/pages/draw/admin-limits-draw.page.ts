import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { TranslateService } from '@ngx-translate/core';
import { forkJoin } from 'rxjs';

import { webAppErrorFromProblemDetail } from '@tch/api';
import type { ProblemDetail } from '@tch/api';
import { TchErrorPanel, TchLoading, TchSectionError } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { AdminEmptyStateComponent } from '@tch/ui/console';

import { DrawAdminApi } from '../../../draw-admin.api.service';
import type { DrawChannelSummary } from '../../../draw-admin.api.service';
import { AdminLimitsApi } from '../../data-access/admin-limits-api.service';
import type { LimitRuleSpec, RuleRow } from '../../data-access/admin-limits.models';
import { LimitAssignmentsTableComponent } from '../../components/limit-assignments-table/limit-assignments-table.component';
import { UpsertLimitDialogComponent } from '../../components/upsert-limit-dialog/upsert-limit-dialog.component';

@Component({
  selector: 'tch-admin-limits-draw-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatSelectModule,
    TchErrorPanel,
    TchLoading,
    TchSectionError,
    AdminEmptyStateComponent,
    LimitAssignmentsTableComponent,
  ],
  templateUrl: './admin-limits-draw.page.html',
  styleUrl: './admin-limits-draw.page.scss',
})
export class AdminLimitsDrawPage implements OnInit {
  private readonly api = inject(AdminLimitsApi);
  private readonly drawApi = inject(DrawAdminApi);
  private readonly dialog = inject(MatDialog);
  private readonly translate = inject(TranslateService);
  private readonly destroyRef = inject(DestroyRef);

  readonly channelCtrl = new FormControl<string | null>(null);
  readonly channels = signal<DrawChannelSummary[]>([]);
  readonly channelsLoading = signal(false);
  readonly channelsError = signal<ErrorViewModel | null>(null);

  readonly loading = signal(false);
  readonly pageError = signal<ErrorViewModel | null>(null);
  readonly actionError = signal<ErrorViewModel | null>(null);
  readonly actionNotice = signal<string | null>(null);
  readonly allRows = signal<RuleRow[]>([]);
  readonly activeRows = computed(() => this.allRows().filter(r => r.assignment !== null));
  readonly unassignedRules = computed<LimitRuleSpec[]>(() => this.allRows().filter(r => !r.assignment).map(r => r.spec));
  readonly selectedChannel = signal<string | null>(null);

  ngOnInit(): void {
    this.loadChannels();
    this.channelCtrl.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(code => {
        this.selectedChannel.set(code);
        this.allRows.set([]);
        this.pageError.set(null);
        this.actionError.set(null);
        this.actionNotice.set(null);
        if (code) this.loadForChannel(code);
      });
  }

  private loadChannels(): void {
    this.channelsLoading.set(true);
    this.drawApi.listChannels().subscribe({
      next: channels => {
        this.channels.set(channels.filter(c => c.active));
        this.channelsLoading.set(false);
      },
      error: (err: unknown) => {
        this.channelsError.set(this.resolveError(err, 'admin.limits.draw.channels', 'section'));
        this.channelsLoading.set(false);
      },
    });
  }

  reload(): void {
    const code = this.selectedChannel();
    if (code) this.loadForChannel(code);
  }

  private loadForChannel(channelCode: string): void {
    this.loading.set(true);
    this.pageError.set(null);
    forkJoin([
      this.api.listRules({ suppressShellFeedback: true }),
      this.api.listAssignments('DRAW_CHANNEL', channelCode, { suppressShellFeedback: true }),
    ]).subscribe({
      next: ([rules, view]) => {
        const assignMap = new Map(view.items.map(a => [a.ruleKey, a]));
        this.allRows.set(rules.map(spec => ({ spec, assignment: assignMap.get(spec.ruleKey) ?? null })));
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.pageError.set(this.resolveError(err, 'admin.limits.draw', 'page'));
        this.loading.set(false);
      },
    });
  }

  openAdd(): void {
    const channelCode = this.selectedChannel();
    if (!channelCode) return;
    const ref = this.dialog.open(UpsertLimitDialogComponent, { width: '560px', maxWidth: '100vw' });
    ref.componentInstance.initAdd(this.unassignedRules(), 'DRAW_CHANNEL', channelCode);
    ref.afterClosed().subscribe((result: unknown) => {
      if (result) {
        this.actionNotice.set('Règle ajoutée.');
        this.reloadAssignments(channelCode);
      }
    });
  }

  openUpsert(row: RuleRow): void {
    const channelCode = this.selectedChannel();
    if (!channelCode) return;
    const ref = this.dialog.open(UpsertLimitDialogComponent, { width: '560px', maxWidth: '100vw' });
    ref.componentInstance.init(row.spec, 'DRAW_CHANNEL', channelCode, row.assignment);
    ref.afterClosed().subscribe((result: unknown) => {
      if (result) {
        this.actionNotice.set('Règle enregistrée.');
        this.reloadAssignments(channelCode);
      }
    });
  }

  confirmDelete(row: RuleRow): void {
    if (!row.assignment) return;
    if (!confirm(`Supprimer la règle « ${row.spec.label || row.spec.ruleKey} » ?`)) return;
    const channelCode = this.selectedChannel();
    if (!channelCode) return;
    this.actionError.set(null);
    this.actionNotice.set(null);
    this.api.deleteAssignment(row.assignment.id.value, { suppressShellFeedback: true }).subscribe({
      next: () => {
        this.actionNotice.set('Règle supprimée.');
        this.reloadAssignments(channelCode);
      },
      error: (err: unknown) => {
        this.actionError.set(this.resolveError(err, 'admin.limits.draw.delete', 'section'));
      },
    });
  }

  private reloadAssignments(channelCode: string): void {
    this.api.listAssignments('DRAW_CHANNEL', channelCode, { suppressShellFeedback: true }).subscribe({
      next: view => {
        const assignMap = new Map(view.items.map(a => [a.ruleKey, a]));
        this.allRows.update(current =>
          current.map(r => ({ spec: r.spec, assignment: assignMap.get(r.spec.ruleKey) ?? null })),
        );
      },
      error: (err: unknown) => {
        this.actionError.set(this.resolveError(err, 'admin.limits.draw.reload', 'section'));
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
