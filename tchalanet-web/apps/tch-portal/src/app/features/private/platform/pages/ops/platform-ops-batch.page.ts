import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';

import { TchLoading, TchErrorPanel } from '@tch/ui/components';
import { AdminPageShellComponent } from '../../../shared/admin-ui/admin-page-shell.component';
import { AdminEmptyStateComponent } from '../../../shared/admin-ui/admin-empty-state.component';
import { AdminStatusPillComponent } from '../../../shared/admin-ui/admin-status-pill.component';
import {
  PlatformOpsApi,
  JobInfoResponse,
  ExecutionResponse,
} from '../../platform-ops-api.service';
import { UpdateGateDialog } from './dialogs/update-gate.dialog';
import { StartJobDialog } from './dialogs/start-job.dialog';

export interface GateRow {
  jobKey: string;
  enabled: boolean;
}

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
    MatIconModule,
    MatSelectModule,
    MatTableModule,
    MatTabsModule,
  ],
  templateUrl: './platform-ops-batch.page.html',
  styleUrls: ['./platform-ops-batch.page.scss'],
})
export class PlatformOpsBatchPage implements OnInit {
  private readonly api = inject(PlatformOpsApi);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly jobColumns = ['job_key', 'display_name', 'scope', 'gate', 'actions'];
  readonly gateColumns = ['jobKey', 'enabled', 'actions'];
  readonly execColumns = ['execution_id', 'job_key', 'status', 'started_at', 'ended_at', 'duration'];

  readonly loadingJobs = signal(false);
  readonly errorJobs = signal<string | null>(null);
  readonly jobs = signal<JobInfoResponse[]>([]);

  readonly loadingGates = signal(false);
  readonly errorGates = signal<string | null>(null);
  private readonly gatesMap = signal<Record<string, boolean>>({});
  readonly gateRows = computed<GateRow[]>(() =>
    Object.entries(this.gatesMap()).map(([jobKey, enabled]) => ({ jobKey, enabled })),
  );

  readonly selectedExecJobKey = signal<string>('');
  readonly loadingExec = signal(false);
  readonly errorExec = signal<string | null>(null);
  readonly executions = signal<ExecutionResponse[]>([]);

  ngOnInit(): void {
    this.loadJobs();
    this.loadGates();
  }

  loadJobs(): void {
    this.loadingJobs.set(true);
    this.api.listJobs().subscribe({
      next: v => { this.jobs.set(v); this.loadingJobs.set(false); },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string } })?.error;
        this.errorJobs.set(pd?.title ?? 'Erreur.');
        this.loadingJobs.set(false);
      },
    });
  }

  loadGates(): void {
    this.loadingGates.set(true);
    this.api.listGates().subscribe({
      next: v => { this.gatesMap.set(v); this.loadingGates.set(false); },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string } })?.error;
        this.errorGates.set(pd?.title ?? 'Erreur.');
        this.loadingGates.set(false);
      },
    });
  }

  openStartJob(job: JobInfoResponse): void {
    const ref = this.dialog.open(StartJobDialog, { data: { job }, width: '500px' });
    ref.afterClosed().subscribe(() => this.loadJobs());
  }

  openToggleGate(row: GateRow): void {
    const ref = this.dialog.open(UpdateGateDialog, {
      data: { jobKey: row.jobKey, enable: !row.enabled },
      width: '460px',
    });
    ref.afterClosed().subscribe((updated) => {
      if (updated) {
        this.snackBar.open(`Gate ${row.jobKey} mis à jour.`, 'OK', { duration: 3000 });
        this.loadGates();
      }
    });
  }

  selectExecJob(jobKey: string): void {
    this.selectedExecJobKey.set(jobKey);
    if (jobKey) this.loadExecutions();
  }

  gateEnabled(jobKey: string): boolean | null {
    return this.gatesMap()[jobKey] ?? null;
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
}
