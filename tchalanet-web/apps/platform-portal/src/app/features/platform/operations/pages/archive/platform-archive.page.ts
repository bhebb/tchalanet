import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateService } from '@ngx-translate/core';

import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { TchErrorPanel, TchLoading, TchSectionError } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { AdminCrudShellComponent } from '@tch/ui/console';
import { AdminEmptyStateComponent } from '@tch/ui/console';
import { AdminPageShellComponent } from '@tch/ui/console';
import { AdminStatusPillComponent, AdminStatusTone } from '@tch/ui/console';
import {
  ArchiveOpsSummary,
  ArchiveRunView,
  PlatformArchiveApi,
  TriggerArchiveRunRequest,
} from '../../data-access/platform-archive-api.service';

// ── Trigger Dialog ─────────────────────────────────────────────────────────

@Component({
  selector: 'tch-trigger-archive-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule, TchSectionError],
  templateUrl: './trigger-archive.dialog.html',
  styleUrls: ['./trigger-archive.dialog.scss'],
})
export class TriggerArchiveDialog {
  private readonly api = inject(PlatformArchiveApi);
  private readonly ref = inject(MatDialogRef<TriggerArchiveDialog>);
  private readonly fb = inject(FormBuilder);
  private readonly translate = inject(TranslateService);

  readonly saving = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly form = this.fb.nonNullable.group({
    strategy: ['', Validators.required],
    periodStart: ['', Validators.required],
    periodEnd: ['', Validators.required],
    reason: ['', [Validators.required, Validators.minLength(10)]],
  });

  save(): void {
    if (this.form.invalid) return;
    this.saving.set(true);
    this.error.set(null);
    const v = this.form.getRawValue();
    const req: TriggerArchiveRunRequest = {
      strategy: v.strategy.toUpperCase(),
      periodStart: v.periodStart,
      periodEnd: v.periodEnd,
      reason: v.reason,
    };
    this.api.triggerRun(req, { suppressShellFeedback: true }).subscribe({
      next: run => this.ref.close(run),
      error: (err: unknown) => {
        this.saving.set(false);
        this.error.set(errorViewModel(err, 'platform.archive.trigger', this.translate));
      },
    });
  }
}

// ── Main Page ──────────────────────────────────────────────────────────────

export type ArchiveView = 'recent' | 'failed' | 'invalid';

@Component({
  selector: 'tch-platform-archive-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    ReactiveFormsModule,
    AdminCrudShellComponent,
    AdminEmptyStateComponent,
    AdminPageShellComponent,
    AdminStatusPillComponent,
    TchErrorPanel,
    TchLoading,
    TchSectionError,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatTableModule,
    MatTooltipModule,
  ],
  templateUrl: './platform-archive.page.html',
  styleUrls: ['./platform-archive.page.scss'],
})
export class PlatformArchivePage implements OnInit {
  private readonly api = inject(PlatformArchiveApi);
  private readonly dialog = inject(MatDialog);
  private readonly translate = inject(TranslateService);

  readonly runColumns = ['startedAt', 'status', 'strategy', 'triggerType', 'duration', 'error'];
  readonly rawColumns = ['key', 'value'];

  readonly loading = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly actionFeedback = signal<ErrorViewModel | null>(null);
  readonly summary = signal<ArchiveOpsSummary | null>(null);
  readonly runs = signal<ArchiveRunView[]>([]);
  readonly rawRows = signal<Record<string, unknown>[]>([]);
  readonly activeView = signal<ArchiveView>('recent');
  readonly expandedId = signal<string | null>(null);

  ngOnInit(): void {
    this.loadSummary();
    this.loadRuns();
  }

  private loadSummary(): void {
    this.api.getOpsSummary({ suppressShellFeedback: true }).subscribe({
      next: s => this.summary.set(s),
      error: err => this.actionFeedback.set(this.errorViewModel(err, 'platform.archive.summary')),
    });
  }

  loadRuns(): void {
    this.loading.set(true);
    this.error.set(null);
    this.activeView.set('recent');
    this.api.listRuns(50, { suppressShellFeedback: true }).subscribe({
      next: list => { this.runs.set(list); this.loading.set(false); },
      error: (err: unknown) => {
        this.error.set(this.errorViewModel(err, 'platform.archive.runs'));
        this.loading.set(false);
      },
    });
  }

  loadFailed(): void {
    this.loading.set(true);
    this.error.set(null);
    this.activeView.set('failed');
    this.api.listFailedRuns(20, { suppressShellFeedback: true }).subscribe({
      next: list => { this.rawRows.set(list); this.loading.set(false); },
      error: (err: unknown) => {
        this.error.set(this.errorViewModel(err, 'platform.archive.failedRuns'));
        this.loading.set(false);
      },
    });
  }

  loadInvalid(): void {
    this.loading.set(true);
    this.error.set(null);
    this.activeView.set('invalid');
    this.api.listInvalidObjects(20, { suppressShellFeedback: true }).subscribe({
      next: list => { this.rawRows.set(list); this.loading.set(false); },
      error: (err: unknown) => {
        this.error.set(this.errorViewModel(err, 'platform.archive.invalidObjects'));
        this.loading.set(false);
      },
    });
  }

  refresh(): void {
    this.loadSummary();
    if (this.activeView() === 'recent') this.loadRuns();
    else if (this.activeView() === 'failed') this.loadFailed();
    else this.loadInvalid();
  }

  openTrigger(): void {
    const ref = this.dialog.open(TriggerArchiveDialog, { width: '520px' });
    ref.afterClosed().subscribe((result: ArchiveRunView | null) => {
      if (result) {
        this.actionFeedback.set({
          title: 'Archive déclenchée',
          message: `Run ${result.id.slice(0, 8)} créé.`,
          severity: 'info',
        });
        this.loadSummary();
        this.loadRuns();
      }
    });
  }

  toggleExpand(id: string): void {
    this.expandedId.set(this.expandedId() === id ? null : id);
  }

  duration(run: ArchiveRunView): string {
    if (!run.completedAt || !run.startedAt) return '—';
    const ms = new Date(run.completedAt).getTime() - new Date(run.startedAt).getTime();
    if (ms < 1000) return `${ms}ms`;
    return `${(ms / 1000).toFixed(1)}s`;
  }

  statusTone(status: string): AdminStatusTone {
    if (status === 'COMPLETED') return 'success';
    if (status === 'FAILED') return 'danger';
    if (status === 'STARTED') return 'warning';
    return 'neutral';
  }

  rawEntries(row: Record<string, unknown>): { key: string; value: string }[] {
    return Object.entries(row).map(([key, value]) => ({
      key,
      value: value == null ? '—' : String(value),
    }));
  }

  private errorViewModel(err: unknown, source: string): ErrorViewModel {
    return errorViewModel(err, source, this.translate);
  }
}

function errorViewModel(err: unknown, source: string, translate: TranslateService): ErrorViewModel {
  const problem = (err as { error?: ProblemDetail })?.error;
  if (problem) {
    const normalized = webAppErrorFromProblemDetail(problem, source, 'page');
    const copy = resolveErrorFeedbackCopy(normalized, key => translate.instant(key));
    return toErrorViewModel(normalized, copy);
  }

  return {
    title: translate.instant('common.errors.fallback.title'),
    message: translate.instant('common.errors.fallback.message'),
    severity: 'error',
  };
}
