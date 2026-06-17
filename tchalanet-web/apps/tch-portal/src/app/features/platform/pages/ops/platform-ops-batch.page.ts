import { ChangeDetectionStrategy, Component, OnInit, inject, signal, computed } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialog, MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';

import { TchLoading, TchErrorPanel } from '@tch/ui/components';
import { AdminPageShellComponent } from '../../../private/shared/admin-ui/admin-page-shell.component';
import { AdminEmptyStateComponent } from '../../../private/shared/admin-ui/admin-empty-state.component';
import { AdminStatusPillComponent } from '../../../private/shared/admin-ui/admin-status-pill.component';
import {
  PlatformOpsApi,
  JobInfoResponse,
  StartJobRequest,
  StartJobResponse,
  GateUpdateRequest,
} from '../../platform-ops-api.service';

// ── Gate update dialog ────────────────────────────────────────────────────────

@Component({
  selector: 'tch-update-gate-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ data.enable ? 'Activer' : 'Désactiver' }} gate</h2>
    <mat-dialog-content>
      <p class="job-key-label"><strong>{{ data.jobKey }}</strong></p>
      <form [formGroup]="form" class="dialog-form">
        <mat-form-field appearance="outline">
          <mat-label>Portée</mat-label>
          <mat-select formControlName="scope">
            <mat-option value="GLOBAL">Global</mat-option>
            <mat-option value="TENANT">Tenant</mat-option>
          </mat-select>
        </mat-form-field>
        @if (form.controls.scope.value === 'TENANT') {
          <mat-form-field appearance="outline">
            <mat-label>Tenant ID</mat-label>
            <input matInput formControlName="tenantId" />
            @if (form.controls.tenantId.invalid && form.controls.tenantId.touched) {
              <mat-error>Requis pour portée TENANT.</mat-error>
            }
          </mat-form-field>
        }
        <mat-form-field appearance="outline">
          <mat-label>Raison</mat-label>
          <input matInput formControlName="reason" />
          @if (form.controls.reason.invalid && form.controls.reason.touched) {
            <mat-error>Requis.</mat-error>
          }
        </mat-form-field>
      </form>
      @if (error()) {
        <div class="error-panel">{{ error() }}</div>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close [disabled]="submitting()">Annuler</button>
      <button mat-flat-button [color]="data.enable ? 'primary' : 'warn'"
        [disabled]="form.invalid || submitting()" (click)="submit()">
        @if (submitting()) { <span class="spin material-symbols-outlined">progress_activity</span> }
        Confirmer
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .job-key-label { background: var(--tch-color-surface-container); padding: 0.5rem 0.75rem; border-radius: 0.375rem; margin-bottom: 1rem; font-size: 0.875rem; }
    .dialog-form { display: flex; flex-direction: column; gap: 0.75rem; min-width: 400px; }
    .error-panel { background: var(--tch-color-error-container); color: var(--tch-color-on-error-container); padding: 0.75rem; border-radius: 0.5rem; font-size: 0.875rem; margin-top: 0.5rem; }
    .spin { animation: spin 0.8s linear infinite; display: inline-block; vertical-align: middle; }
    @keyframes spin { to { transform: rotate(360deg); } }
  `],
})
export class UpdateGateDialog {
  protected readonly data = inject<{ jobKey: string; enable: boolean }>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<UpdateGateDialog>);
  private readonly api = inject(PlatformOpsApi);
  private readonly fb = inject(FormBuilder);

  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.group({
    scope: ['GLOBAL' as 'GLOBAL' | 'TENANT'],
    tenantId: [''],
    reason: ['', Validators.required],
  });

  submit(): void {
    if (this.form.invalid || this.submitting()) return;
    const v = this.form.value;
    if (v.scope === 'TENANT' && !v.tenantId) {
      this.form.controls.tenantId.setValidators(Validators.required);
      this.form.controls.tenantId.updateValueAndValidity();
      this.form.markAllAsTouched();
      return;
    }
    const req: GateUpdateRequest = {
      scope: v.scope!,
      tenant_id: v.scope === 'TENANT' ? v.tenantId : null,
      enabled: this.data.enable,
      reason: v.reason!,
    };
    this.submitting.set(true);
    this.error.set(null);
    this.api.updateGate(this.data.jobKey, req).subscribe({
      next: () => { this.submitting.set(false); this.dialogRef.close(true); },
      error: (err: unknown) => {
        this.submitting.set(false);
        const pd = (err as { error?: { title?: string } })?.error;
        this.error.set(pd?.title ?? 'Erreur lors de la mise à jour.');
      },
    });
  }
}

// ── Start job dialog ──────────────────────────────────────────────────────────

@Component({
  selector: 'tch-start-job-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
  ],
  template: `
    <h2 mat-dialog-title>Démarrer le job</h2>
    <mat-dialog-content>
      <p class="job-key-label"><strong>{{ data.job.job_key }}</strong> — {{ data.job.display_name }}</p>
      @if (data.job.required_params?.length) {
        <p class="hint">Paramètres requis: {{ data.job.required_params.join(', ') }}</p>
      }
      @if (data.job.optional_params?.length) {
        <p class="hint">Paramètres optionnels: {{ data.job.optional_params.join(', ') }}</p>
      }
      <form [formGroup]="form" class="dialog-form">
        <mat-form-field appearance="outline">
          <mat-label>Paramètres JSON (clés snake_case)</mat-label>
          <textarea matInput formControlName="paramsJson" rows="4" placeholder='{}'></textarea>
          @if (form.controls.paramsJson.invalid && form.controls.paramsJson.touched) {
            <mat-error>JSON invalide.</mat-error>
          }
        </mat-form-field>
      </form>
      @if (result()) {
        <div class="result-panel">
          Job démarré — Exécution #{{ result()!.execution_id }} · Statut: {{ result()!.status }}
        </div>
      }
      @if (error()) {
        <div class="error-panel">{{ error() }}</div>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="dialogRef.close()">{{ result() ? 'Fermer' : 'Annuler' }}</button>
      @if (!result()) {
        <button mat-flat-button color="primary" [disabled]="form.invalid || submitting()" (click)="submit()">
          @if (submitting()) { <span class="spin material-symbols-outlined">progress_activity</span> }
          Démarrer
        </button>
      }
    </mat-dialog-actions>
  `,
  styles: [`
    .job-key-label { background: var(--tch-color-surface-container); padding: 0.5rem 0.75rem; border-radius: 0.375rem; margin-bottom: 0.5rem; font-size: 0.875rem; }
    .hint { font-size: 0.8125rem; color: var(--tch-color-on-surface-variant); margin: 0 0 0.5rem; }
    .dialog-form { display: flex; flex-direction: column; gap: 0.75rem; min-width: 420px; }
    .result-panel { background: var(--tch-color-success-container, #d4edda); color: var(--tch-color-on-success-container, #155724); padding: 0.75rem; border-radius: 0.5rem; font-size: 0.875rem; margin-top: 0.75rem; }
    .error-panel { background: var(--tch-color-error-container); color: var(--tch-color-on-error-container); padding: 0.75rem; border-radius: 0.5rem; font-size: 0.875rem; margin-top: 0.5rem; }
    .spin { animation: spin 0.8s linear infinite; display: inline-block; vertical-align: middle; }
    @keyframes spin { to { transform: rotate(360deg); } }
  `],
})
export class StartJobDialog {
  protected readonly data = inject<{ job: JobInfoResponse }>(MAT_DIALOG_DATA);
  readonly dialogRef = inject(MatDialogRef<StartJobDialog>);
  private readonly api = inject(PlatformOpsApi);
  private readonly fb = inject(FormBuilder);

  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  readonly result = signal<StartJobResponse | null>(null);

  readonly form = this.fb.group({
    paramsJson: ['{}'],
  });

  submit(): void {
    if (this.submitting() || this.result()) return;
    let params: Record<string, string>;
    try {
      params = JSON.parse(this.form.value.paramsJson ?? '{}') as Record<string, string>;
    } catch {
      this.form.controls.paramsJson.setValidators([Validators.required]);
      this.form.controls.paramsJson.setErrors({ invalidJson: true });
      this.form.markAllAsTouched();
      return;
    }
    const req: StartJobRequest = { params };
    this.submitting.set(true);
    this.error.set(null);
    this.api.startJob(this.data.job.job_key, req).subscribe({
      next: (res) => { this.submitting.set(false); this.result.set(res); },
      error: (err: unknown) => {
        this.submitting.set(false);
        const pd = (err as { error?: { title?: string } })?.error;
        this.error.set(pd?.title ?? 'Erreur lors du démarrage.');
      },
    });
  }
}

// ── Main page ─────────────────────────────────────────────────────────────────

interface GateRow {
  jobKey: string;
  enabled: boolean;
}

@Component({
  selector: 'tch-platform-ops-batch-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminPageShellComponent,
    AdminEmptyStateComponent,
    AdminStatusPillComponent,
    TchLoading,
    TchErrorPanel,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatTabsModule,
  ],
  template: `
    <tch-admin-page-shell title="Opérations Batch" description="Jobs planifiés et gates de contrôle.">
      <mat-tab-group>

        <!-- JOBS TAB -->
        <mat-tab label="Jobs">
          <div class="tab-content">
            @if (loadingJobs()) {
              <tch-loading label="Chargement..." />
            } @else if (errorJobs()) {
              <tch-error-panel [title]="errorJobs()!" [showRetry]="true" retryLabel="Réessayer" (retry)="loadJobs()" />
            } @else if (jobs().length === 0) {
              <tch-admin-empty-state icon="schedule" title="Aucun job" message="Aucun job configuré." />
            } @else {
              <table mat-table [dataSource]="jobs()">
                <ng-container matColumnDef="job_key">
                  <th mat-header-cell *matHeaderCellDef>Clé</th>
                  <td mat-cell *matCellDef="let row"><code>{{ row.job_key }}</code></td>
                </ng-container>
                <ng-container matColumnDef="display_name">
                  <th mat-header-cell *matHeaderCellDef>Nom</th>
                  <td mat-cell *matCellDef="let row">{{ row.display_name }}</td>
                </ng-container>
                <ng-container matColumnDef="scope">
                  <th mat-header-cell *matHeaderCellDef>Portée</th>
                  <td mat-cell *matCellDef="let row">{{ row.scope }}</td>
                </ng-container>
                <ng-container matColumnDef="actions">
                  <th mat-header-cell *matHeaderCellDef></th>
                  <td mat-cell *matCellDef="let row">
                    <button mat-stroked-button (click)="openStartJob(row)">
                      <span class="material-symbols-outlined">play_arrow</span>
                      Start
                    </button>
                  </td>
                </ng-container>
                <tr mat-header-row *matHeaderRowDef="jobColumns"></tr>
                <tr mat-row *matRowDef="let row; columns: jobColumns"></tr>
              </table>
            }
          </div>
        </mat-tab>

        <!-- GATES TAB -->
        <mat-tab label="Gates">
          <div class="tab-content">
            @if (loadingGates()) {
              <tch-loading label="Chargement..." />
            } @else if (errorGates()) {
              <tch-error-panel [title]="errorGates()!" [showRetry]="true" retryLabel="Réessayer" (retry)="loadGates()" />
            } @else if (gateRows().length === 0) {
              <tch-admin-empty-state icon="toggle_off" title="Aucun gate" message="Aucun gate configuré." />
            } @else {
              <table mat-table [dataSource]="gateRows()">
                <ng-container matColumnDef="jobKey">
                  <th mat-header-cell *matHeaderCellDef>Clé</th>
                  <td mat-cell *matCellDef="let row"><code>{{ row.jobKey }}</code></td>
                </ng-container>
                <ng-container matColumnDef="enabled">
                  <th mat-header-cell *matHeaderCellDef>Statut</th>
                  <td mat-cell *matCellDef="let row">
                    <tch-admin-status-pill [tone]="row.enabled ? 'success' : 'danger'" [label]="row.enabled ? 'Activé' : 'Désactivé'" />
                  </td>
                </ng-container>
                <ng-container matColumnDef="actions">
                  <th mat-header-cell *matHeaderCellDef></th>
                  <td mat-cell *matCellDef="let row">
                    <button mat-stroked-button [color]="row.enabled ? 'warn' : 'primary'"
                      (click)="openToggleGate(row)">
                      {{ row.enabled ? 'Désactiver' : 'Activer' }}
                    </button>
                  </td>
                </ng-container>
                <tr mat-header-row *matHeaderRowDef="gateColumns"></tr>
                <tr mat-row *matRowDef="let row; columns: gateColumns"></tr>
              </table>
            }
          </div>
        </mat-tab>

      </mat-tab-group>
    </tch-admin-page-shell>
  `,
  styles: [`
    .tab-content { padding: 1rem 0; }
    table { width: 100%; }
    code { font-family: monospace; font-size: 0.8125rem; }
  `],
})
export class PlatformOpsBatchPage implements OnInit {
  private readonly api = inject(PlatformOpsApi);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly jobColumns = ['job_key', 'display_name', 'scope', 'actions'];
  readonly gateColumns = ['jobKey', 'enabled', 'actions'];

  readonly loadingJobs = signal(false);
  readonly errorJobs = signal<string | null>(null);
  readonly jobs = signal<JobInfoResponse[]>([]);

  readonly loadingGates = signal(false);
  readonly errorGates = signal<string | null>(null);
  private readonly gatesMap = signal<Record<string, boolean>>({});
  readonly gateRows = computed<GateRow[]>(() =>
    Object.entries(this.gatesMap()).map(([jobKey, enabled]) => ({ jobKey, enabled })),
  );

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
}
