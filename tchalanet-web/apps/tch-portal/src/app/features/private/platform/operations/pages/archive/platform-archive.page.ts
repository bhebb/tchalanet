import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe, KeyValuePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';

import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { AdminCrudShellComponent } from '../../../../shared/admin-ui/admin-crud-shell.component';
import { AdminEmptyStateComponent } from '../../../../shared/admin-ui/admin-empty-state.component';
import { AdminPageShellComponent } from '../../../../shared/admin-ui/admin-page-shell.component';
import { AdminStatusPillComponent, AdminStatusTone } from '../../../../shared/admin-ui/admin-status-pill.component';
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
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>Déclencher une archive</h2>
    <mat-dialog-content>
      <form [formGroup]="form" style="display:flex;flex-direction:column;gap:12px;padding-top:8px">
        <mat-form-field appearance="outline">
          <mat-label>Stratégie (strategy)</mat-label>
          <input matInput formControlName="strategy" placeholder="ex: AUDIT_LOG_COLD" />
          @if (form.controls.strategy.invalid && form.controls.strategy.touched) {
            <mat-error>Requis.</mat-error>
          }
        </mat-form-field>
        <div style="display:flex;gap:12px">
          <mat-form-field appearance="outline" style="flex:1">
            <mat-label>Début de période</mat-label>
            <input matInput type="date" formControlName="periodStart" />
            @if (form.controls.periodStart.invalid && form.controls.periodStart.touched) {
              <mat-error>Requis.</mat-error>
            }
          </mat-form-field>
          <mat-form-field appearance="outline" style="flex:1">
            <mat-label>Fin de période</mat-label>
            <input matInput type="date" formControlName="periodEnd" />
            @if (form.controls.periodEnd.invalid && form.controls.periodEnd.touched) {
              <mat-error>Requis.</mat-error>
            }
          </mat-form-field>
        </div>
        <mat-form-field appearance="outline">
          <mat-label>Raison (min. 10 caractères)</mat-label>
          <textarea matInput formControlName="reason" rows="3" placeholder="Raison opérationnelle de l'archivage..."></textarea>
          @if (form.controls.reason.invalid && form.controls.reason.touched) {
            <mat-error>Requis (min. 10 caractères).</mat-error>
          }
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button mat-dialog-close>Annuler</button>
      <button mat-flat-button color="primary" [disabled]="form.invalid || saving()" (click)="save()">
        Déclencher
      </button>
    </mat-dialog-actions>
  `,
})
export class TriggerArchiveDialog {
  private readonly api = inject(PlatformArchiveApi);
  private readonly ref = inject(MatDialogRef<TriggerArchiveDialog>);
  private readonly fb = inject(FormBuilder);

  readonly saving = signal(false);
  readonly form = this.fb.nonNullable.group({
    strategy: ['', Validators.required],
    periodStart: ['', Validators.required],
    periodEnd: ['', Validators.required],
    reason: ['', [Validators.required, Validators.minLength(10)]],
  });

  save(): void {
    if (this.form.invalid) return;
    this.saving.set(true);
    const v = this.form.getRawValue();
    const req: TriggerArchiveRunRequest = {
      strategy: v.strategy.toUpperCase(),
      periodStart: v.periodStart,
      periodEnd: v.periodEnd,
      reason: v.reason,
    };
    this.api.triggerRun(req).subscribe({
      next: run => this.ref.close(run),
      error: (err: unknown) => { this.ref.close({ __error: (err as { error?: { title?: string } })?.error?.title ?? 'Erreur.' }); },
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
    KeyValuePipe,
    ReactiveFormsModule,
    AdminCrudShellComponent,
    AdminEmptyStateComponent,
    AdminPageShellComponent,
    AdminStatusPillComponent,
    TchErrorPanel,
    TchLoading,
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
  private readonly snackBar = inject(MatSnackBar);

  readonly runColumns = ['startedAt', 'status', 'strategy', 'triggerType', 'duration', 'error'];
  readonly rawColumns = ['key', 'value'];

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
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
    this.api.getOpsSummary().subscribe({ next: s => this.summary.set(s) });
  }

  loadRuns(): void {
    this.loading.set(true);
    this.error.set(null);
    this.activeView.set('recent');
    this.api.listRuns(50).subscribe({
      next: list => { this.runs.set(list); this.loading.set(false); },
      error: (err: unknown) => {
        this.error.set((err as { error?: { title?: string } })?.error?.title ?? 'Erreur de chargement.');
        this.loading.set(false);
      },
    });
  }

  loadFailed(): void {
    this.loading.set(true);
    this.error.set(null);
    this.activeView.set('failed');
    this.api.listFailedRuns(20).subscribe({
      next: list => { this.rawRows.set(list); this.loading.set(false); },
      error: (err: unknown) => {
        this.error.set((err as { error?: { title?: string } })?.error?.title ?? 'Erreur.');
        this.loading.set(false);
      },
    });
  }

  loadInvalid(): void {
    this.loading.set(true);
    this.error.set(null);
    this.activeView.set('invalid');
    this.api.listInvalidObjects(20).subscribe({
      next: list => { this.rawRows.set(list); this.loading.set(false); },
      error: (err: unknown) => {
        this.error.set((err as { error?: { title?: string } })?.error?.title ?? 'Erreur.');
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
    ref.afterClosed().subscribe((result: ArchiveRunView | { __error: string } | null) => {
      if (result && '__error' in result) {
        this.error.set(result.__error);
        setTimeout(() => window.scrollTo({ top: 0, behavior: 'smooth' }), 50);
        return;
      }
      if (result) {
        this.snackBar.open(`Archive déclenchée — run ${result.id.slice(0, 8)}…`, 'OK', { duration: 5000 });
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
}
