import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';

import { TchLoading, TchErrorPanel } from '@tch/ui/components';
import { AdminPageShellComponent } from '../../../shared/admin-ui/admin-page-shell.component';
import { AdminEmptyStateComponent } from '../../../shared/admin-ui/admin-empty-state.component';
import { AdminStatusPillComponent } from '../../../shared/admin-ui/admin-status-pill.component';
import {
  PlatformOpsApi,
  JobInfoResponse,
  ExecutionResponse,
  StartJobResponse,
} from '../../platform-ops-api.service';
import { StartJobDialog } from './dialogs/start-job.dialog';

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
  private readonly snackBar = inject(MatSnackBar);

  readonly jobColumns = ['job_key', 'display_name', 'scope', 'params', 'actions'];
  readonly execColumns = ['execution_id', 'status', 'started_at', 'ended_at', 'duration', 'context', 'outcome', 'actions'];

  readonly loadingJobs = signal(false);
  readonly errorJobs = signal<string | null>(null);
  readonly jobs = signal<JobInfoResponse[]>([]);

  readonly selectedExecJobKey = signal<string>('');
  readonly loadingExec = signal(false);
  readonly errorExec = signal<string | null>(null);
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
    this.api.listJobs().subscribe({
      next: v => {
        this.jobs.set(v);
        if (!this.selectedExecJobKey() && v.length) {
          this.selectedExecJobKey.set(v[0].job_key);
          this.loadExecutions();
        }
        this.loadingJobs.set(false);
      },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string } })?.error;
        this.errorJobs.set(pd?.title ?? 'Erreur.');
        this.loadingJobs.set(false);
      },
    });
  }

  openStartJob(job: JobInfoResponse): void {
    const ref = this.dialog.open(StartJobDialog, { data: { job }, width: '500px' });
    ref.afterClosed().subscribe((started: StartJobResponse | null) => {
      if (started) {
        this.lastLaunch.set(started);
        this.snackBar.open(`Job démarré: exécution #${started.execution_id}`, 'OK', { duration: 4000 });
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
    this.api.listExecutions(jobKey, 50).subscribe({
      next: v => { this.executions.set(v); this.loadingExec.set(false); },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string } })?.error;
        this.errorExec.set(pd?.title ?? 'Erreur.');
        this.loadingExec.set(false);
      },
    });
  }

  restartExecution(row: ExecutionResponse): void {
    this.api.restartExecution(row.execution_id).subscribe({
      next: res => {
        this.lastLaunch.set(res);
        this.snackBar.open(`Restart lancé: exécution #${res.execution_id}`, 'OK', { duration: 4000 });
        this.selectedExecJobKey.set(res.job_key);
        this.loadExecutions();
      },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string } })?.error;
        this.snackBar.open(pd?.title ?? 'Restart impossible.', 'OK', { duration: 5000 });
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
}

@Component({
  selector: 'tch-execution-details-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, AdminStatusPillComponent, MatButtonModule, MatDialogModule, MatIconModule],
  template: `
    <h2 mat-dialog-title>Exécution #{{ execution.execution_id }}</h2>
    <mat-dialog-content>
      <dl class="execution-details__meta">
        <div>
          <dt>Job</dt>
          <dd><code>{{ execution.job_key }}</code></dd>
        </div>
        <div>
          <dt>Statut</dt>
          <dd><tch-admin-status-pill [tone]="statusTone(execution.status)" [label]="execution.status" /></dd>
        </div>
        <div>
          <dt>Début</dt>
          <dd>{{ execution.started_at | date:'dd/MM/yyyy HH:mm:ss' }}</dd>
        </div>
        <div>
          <dt>Fin</dt>
          <dd>{{ execution.ended_at ? (execution.ended_at | date:'dd/MM/yyyy HH:mm:ss') : '—' }}</dd>
        </div>
        <div>
          <dt>Contexte</dt>
          <dd>{{ execution.context || '—' }}</dd>
        </div>
        <div>
          <dt>Exit code</dt>
          <dd>{{ execution.exit_code || '—' }}</dd>
        </div>
      </dl>

      <section class="execution-details__section">
        <h3>Résultat</h3>
        <pre>{{ execution.exit_message || execution.exit_code || '—' }}</pre>
      </section>

      <section class="execution-details__section">
        <h3>Données copiables</h3>
        <pre>{{ payload }}</pre>
      </section>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button type="button" (click)="copy()">
        <span class="material-symbols-outlined" aria-hidden="true">content_copy</span>
        Copier
      </button>
      <button mat-flat-button color="primary" mat-dialog-close>Fermer</button>
    </mat-dialog-actions>
  `,
  styles: [`
    .execution-details__meta {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(13rem, 1fr));
      gap: 0.75rem;
      margin: 0 0 1rem;
    }

    .execution-details__meta div {
      display: grid;
      gap: 0.25rem;
      min-width: 0;
    }

    .execution-details__meta dt,
    .execution-details__section h3 {
      margin: 0;
      color: var(--tch-color-on-surface-variant);
      font-size: var(--tch-font-size-label-sm);
      font-weight: var(--tch-weight-semibold);
    }

    .execution-details__meta dd {
      margin: 0;
      color: var(--tch-color-on-surface);
      overflow-wrap: anywhere;
    }

    .execution-details__section {
      display: grid;
      gap: 0.5rem;
      margin-top: 1rem;
    }

    .execution-details__section pre {
      max-height: 18rem;
      margin: 0;
      overflow: auto;
      white-space: pre-wrap;
      overflow-wrap: anywhere;
      border: 1px solid var(--tch-color-outline-variant);
      border-radius: var(--tch-radius-sm);
      padding: 0.75rem;
      background: var(--tch-color-surface-container);
      color: var(--tch-color-on-surface);
      font-family: monospace;
      font-size: var(--tch-font-size-body-sm);
    }
  `],
})
class ExecutionDetailsDialog {
  private readonly data = inject<{ execution: ExecutionResponse }>(MAT_DIALOG_DATA);
  private readonly snackBar = inject(MatSnackBar);

  readonly execution = this.data.execution;
  readonly payload = JSON.stringify(this.execution, null, 2);

  copy(): void {
    void navigator.clipboard.writeText(this.payload).then(
      () => this.snackBar.open('Détail copié.', 'OK', { duration: 2500 }),
      () => this.snackBar.open('Copie impossible depuis ce navigateur.', 'OK', { duration: 4000 }),
    );
  }

  statusTone(status: string): 'success' | 'danger' | 'warning' | 'neutral' | 'info' {
    return statusTone(status);
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
