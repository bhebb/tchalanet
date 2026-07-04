import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { TranslateService } from '@ngx-translate/core';

import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { TchLoading, TchErrorPanel, TchSectionError } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { AdminPageShellComponent } from '@tch/ui/console';
import { AdminEmptyStateComponent } from '@tch/ui/console';
import { AdminStatusPillComponent } from '@tch/ui/console';
import {
  PlatformOpsApi,
  JobInfoResponse,
  ExecutionResponse,
  StartJobResponse,
} from '../../data-access/platform-ops-api.service';
import { ExecutionDetailsDialog } from '../../components/dialogs/execution-details.dialog';
import { StartJobDialog } from '../../components/dialogs/start-job.dialog';

@Component({
  selector: 'tch-platform-ops-batch-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    AdminPageShellComponent,
    AdminEmptyStateComponent,
    AdminStatusPillComponent,
    TchLoading,
    TchErrorPanel,
    TchSectionError,
    MatButtonModule,
    MatDialogModule,
    MatIconModule,
    MatSelectModule,
    MatTableModule,
  ],
  templateUrl: './platform-ops-batch.page.html',
  styleUrls: ['./platform-ops-batch.page.scss'],
})
export class PlatformOpsBatchPage implements OnInit {
  private readonly api = inject(PlatformOpsApi);
  private readonly dialog = inject(MatDialog);
  private readonly translate = inject(TranslateService);

  readonly jobColumns = ['job_key', 'display_name', 'scope', 'params', 'actions'];
  readonly execColumns = ['execution_id', 'status', 'started_at', 'ended_at', 'duration', 'context', 'outcome', 'actions'];

  readonly loadingJobs = signal(false);
  readonly errorJobs = signal<ErrorViewModel | null>(null);
  readonly actionError = signal<ErrorViewModel | null>(null);
  readonly actionNotice = signal<{ title: string; message: string } | null>(null);
  readonly jobs = signal<JobInfoResponse[]>([]);

  readonly selectedExecJobKey = signal<string>('');
  readonly loadingExec = signal(false);
  readonly errorExec = signal<ErrorViewModel | null>(null);
  readonly executions = signal<ExecutionResponse[]>([]);
  readonly lastLaunch = signal<StartJobResponse | null>(null);
  readonly selectedJob = computed(() =>
    this.jobs().find(job => job.job_key === this.selectedExecJobKey()) ?? null,
  );

  ngOnInit(): void {
    this.loadJobs();
  }

  loadJobs(): void {
    this.loadingJobs.set(true);
    this.errorJobs.set(null);
    this.actionError.set(null);
    this.api.listJobs({ suppressShellFeedback: true }).subscribe({
      next: v => {
        this.jobs.set(v);
        if (!this.selectedExecJobKey() && v.length) {
          this.selectedExecJobKey.set(v[0].job_key);
          this.loadExecutions();
        }
        this.loadingJobs.set(false);
      },
      error: (err: unknown) => {
        this.errorJobs.set(this.errorViewModel(err, 'platform.ops.batch.jobs'));
        this.loadingJobs.set(false);
      },
    });
  }

  openStartJob(job: JobInfoResponse): void {
    const ref = this.dialog.open(StartJobDialog, { data: { job }, width: '500px' });
    ref.afterClosed().subscribe((started: StartJobResponse | null) => {
      if (started) {
        this.lastLaunch.set(started);
        this.actionError.set(null);
        this.actionNotice.set({
          title: 'Job démarré',
          message: `Exécution #${started.execution_id}`,
        });
        this.selectedExecJobKey.set(job.job_key);
        this.loadExecutions();
      }
    });
  }

  openHistory(job: JobInfoResponse): void {
    this.selectExecJob(job.job_key);
  }

  selectExecJob(jobKey: string): void {
    this.selectedExecJobKey.set(jobKey);
    if (jobKey) this.loadExecutions();
  }

  executionDuration(row: ExecutionResponse): string {
    if (!row.started_at || !row.ended_at) return '—';
    const started = new Date(row.started_at).getTime();
    const ended = new Date(row.ended_at).getTime();
    if (Number.isNaN(started) || Number.isNaN(ended) || ended < started) return '—';
    const seconds = Math.round((ended - started) / 1000);
    if (seconds < 60) return `${seconds}s`;
    const minutes = Math.floor(seconds / 60);
    const rest = seconds % 60;
    return `${minutes}m ${rest}s`;
  }

  loadExecutions(): void {
    const jobKey = this.selectedExecJobKey();
    if (!jobKey) return;
    this.loadingExec.set(true);
    this.errorExec.set(null);
    this.api.listExecutions(jobKey, 50, { suppressShellFeedback: true }).subscribe({
      next: v => { this.executions.set(v); this.loadingExec.set(false); },
      error: (err: unknown) => {
        this.errorExec.set(this.errorViewModel(err, 'platform.ops.batch.executions'));
        this.loadingExec.set(false);
      },
    });
  }

  restartExecution(row: ExecutionResponse): void {
    this.actionError.set(null);
    this.actionNotice.set(null);
    this.api.restartExecution(row.execution_id, { suppressShellFeedback: true }).subscribe({
      next: res => {
        this.lastLaunch.set(res);
        this.actionNotice.set({
          title: 'Restart lancé',
          message: `Exécution #${res.execution_id}`,
        });
        this.selectedExecJobKey.set(res.job_key);
        this.loadExecutions();
      },
      error: (err: unknown) => {
        this.actionError.set(this.errorViewModel(err, 'platform.ops.batch.restart'));
      },
    });
  }

  canRestart(row: ExecutionResponse): boolean {
    return ['FAILED', 'STOPPED', 'ABANDONED'].includes(row.status);
  }

  executionOutcome(row: ExecutionResponse): string {
    return row.exit_message || row.exit_code || '—';
  }

  openExecutionDetails(row: ExecutionResponse): void {
    this.dialog.open(ExecutionDetailsDialog, {
      data: { execution: row },
      width: '720px',
    });
  }

  clearLastLaunch(): void {
    this.lastLaunch.set(null);
  }

  statusTone(status: string): 'success' | 'danger' | 'warning' | 'neutral' | 'info' {
    return statusTone(status);
  }

  paramsLabel(job: JobInfoResponse): string {
    const required = job.required_params?.length ?? 0;
    const optional = job.optional_params?.length ?? 0;
    if (!required && !optional) return 'Aucun';
    return `${required} requis · ${optional} optionnel(s)`;
  }

  private errorViewModel(err: unknown, source: string): ErrorViewModel {
    const problem = (err as { error?: ProblemDetail })?.error;
    if (problem) {
      const normalized = webAppErrorFromProblemDetail(problem, source, 'page');
      const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
      return toErrorViewModel(normalized, copy);
    }

    return {
      title: this.translate.instant('common.errors.fallback.title'),
      message: this.translate.instant('common.errors.fallback.message'),
      severity: 'error',
    };
  }
}

function statusTone(status: string): 'success' | 'danger' | 'warning' | 'neutral' | 'info' {
  switch (status) {
    case 'COMPLETED':
    case 'SUCCESS':
      return 'success';
    case 'FAILED':
      return 'danger';
    case 'STARTED':
    case 'STARTING':
    case 'STOPPING':
      return 'warning';
    case 'UNKNOWN':
    case 'STOPPED':
    case 'ABANDONED':
      return 'neutral';
    default:
      return 'info';
  }
}
