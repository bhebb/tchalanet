import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialog, MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';

import { AdminPageShellComponent } from '../../../private/shared/admin-ui/admin-page-shell.component';
import { AdminEmptyStateComponent } from '../../../private/shared/admin-ui/admin-empty-state.component';
import {
  PlatformOpsApi,
  BatchJobView,
  BatchGateView,
  BatchExecutionView,
  JobKey,
  StartJobRequest,
} from '../../platform-ops-api.service';

// ── Inline dialog ─────────────────────────────────────────────────────────────

@Component({
  selector: 'tch-ops-start-job-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
  ],
  template: `
    <h2 mat-dialog-title>Démarrer le job</h2>
    <mat-dialog-content>
      <p class="job-key-label">
        <strong>{{ data.jobKey }}</strong>
      </p>
      <form [formGroup]="form" class="dialog-form">
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Code tenant (optionnel)</mat-label>
          <input matInput formControlName="tenantCode" />
        </mat-form-field>

        <mat-checkbox formControlName="dryRun">Dry-run (simuler sans appliquer)</mat-checkbox>
        <mat-checkbox formControlName="force">Forcer l'exécution</mat-checkbox>

        @if (form.controls.force.value) {
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Raison (requis si force=true)</mat-label>
            <input matInput formControlName="reason" />
            @if (form.controls.reason.invalid && form.controls.reason.touched) {
              <mat-error>Raison requise.</mat-error>
            }
          </mat-form-field>
        }

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Paramètres JSON (optionnel)</mat-label>
          <textarea matInput formControlName="parameters" rows="3" placeholder='{}'></textarea>
        </mat-form-field>
      </form>

      @if (error()) {
        <div class="error-panel">
          <span class="material-symbols-outlined">error</span>
          {{ error() }}
        </div>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close [disabled]="submitting()">Annuler</button>
      <button
        mat-flat-button
        color="primary"
        [disabled]="form.invalid || submitting()"
        (click)="submit()"
      >
        @if (submitting()) {
          <span class="material-symbols-outlined spin">progress_activity</span>
        }
        Démarrer
      </button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      .job-key-label {
        font-size: 0.9rem;
        background: var(--tch-color-surface-container);
        padding: 0.5rem 0.75rem;
        border-radius: 0.375rem;
        margin-bottom: 1rem;
      }
      .dialog-form {
        display: flex;
        flex-direction: column;
        gap: 0.75rem;
        min-width: 400px;
      }
      .full-width {
        width: 100%;
      }
      .error-panel {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        padding: 0.75rem;
        border-radius: 0.5rem;
        background: var(--tch-color-error-container, #ffdad6);
        color: var(--tch-color-on-error-container, #410002);
        font-size: 0.875rem;
        margin-top: 0.5rem;
      }
      .spin {
        animation: spin 0.8s linear infinite;
        display: inline-block;
        vertical-align: middle;
      }
      @keyframes spin { to { transform: rotate(360deg); } }
    `,
  ],
})
export class OpsStartJobDialog {
  protected readonly data = inject<{ jobKey: JobKey }>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<OpsStartJobDialog>);
  private readonly api = inject(PlatformOpsApi);
  private readonly snackBar = inject(MatSnackBar);
  private readonly fb = inject(FormBuilder);

  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.group({
    tenantCode: [''],
    dryRun: [true],
    force: [false],
    reason: [''],
    parameters: [''],
  });

  submit(): void {
    if (this.submitting()) return;

    const v = this.form.value;
    if (v.force && !v.reason) {
      this.form.controls.reason.setValidators(Validators.required);
      this.form.controls.reason.updateValueAndValidity();
      this.form.markAllAsTouched();
      return;
    }

    let parameters: Record<string, unknown> | undefined;
    if (v.parameters) {
      try {
        parameters = JSON.parse(v.parameters) as Record<string, unknown>;
      } catch {
        this.error.set('Paramètres JSON invalides.');
        return;
      }
    }

    const req: StartJobRequest = {
      jobKey: this.data.jobKey,
      tenantCode: v.tenantCode || undefined,
      dryRun: v.dryRun ?? true,
      force: v.force ?? false,
      reason: v.reason || undefined,
      parameters,
    };

    this.submitting.set(true);
    this.error.set(null);

    this.api.startJob(this.data.jobKey, req).subscribe({
      next: () => {
        this.submitting.set(false);
        this.snackBar.open('Job démarré.', 'OK', { duration: 3000 });
        this.dialogRef.close(true);
      },
      error: (err: unknown) => {
        this.submitting.set(false);
        const pd = (err as { error?: { title?: string } })?.error;
        this.error.set(pd?.title ?? 'Erreur lors du démarrage.');
      },
    });
  }
}

// ── Main page ─────────────────────────────────────────────────────────────────

@Component({
  selector: 'tch-platform-ops-batch-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminPageShellComponent,
    AdminEmptyStateComponent,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatTabsModule,
  ],
  template: `
    <tch-admin-page-shell title="Opérations Batch" description="Gestion des jobs planifiés.">
      <mat-tab-group>
        <!-- JOBS TAB -->
        <mat-tab label="Jobs">
          @if (loadingJobs()) {
            <div class="loading-state">
              <span class="material-symbols-outlined spin">progress_activity</span>
              Chargement...
            </div>
          } @else if (errorJobs()) {
            <div class="error-panel">
              <span class="material-symbols-outlined">error</span>
              {{ errorJobs() }}
            </div>
          } @else if (jobs().length === 0) {
            <tch-admin-empty-state icon="schedule" title="Aucun job" message="Aucun job configuré." />
          } @else {
            <table mat-table [dataSource]="jobs()">
              <ng-container matColumnDef="jobKey">
                <th mat-header-cell *matHeaderCellDef>Clé</th>
                <td mat-cell *matCellDef="let row">{{ row.jobKey }}</td>
              </ng-container>
              <ng-container matColumnDef="status">
                <th mat-header-cell *matHeaderCellDef>Statut</th>
                <td mat-cell *matCellDef="let row">{{ row.status }}</td>
              </ng-container>
              <ng-container matColumnDef="lastRunAt">
                <th mat-header-cell *matHeaderCellDef>Dernier run</th>
                <td mat-cell *matCellDef="let row">{{ row.lastRunAt ?? '—' }}</td>
              </ng-container>
              <ng-container matColumnDef="nextRunAt">
                <th mat-header-cell *matHeaderCellDef>Prochain run</th>
                <td mat-cell *matCellDef="let row">{{ row.nextRunAt ?? '—' }}</td>
              </ng-container>
              <ng-container matColumnDef="actions">
                <th mat-header-cell *matHeaderCellDef></th>
                <td mat-cell *matCellDef="let row">
                  <button mat-stroked-button (click)="openStartJobDialog(row.jobKey)">
                    <span class="material-symbols-outlined">play_arrow</span>
                    Start
                  </button>
                </td>
              </ng-container>
              <tr mat-header-row *matHeaderRowDef="jobColumns"></tr>
              <tr mat-row *matRowDef="let row; columns: jobColumns"></tr>
            </table>
          }
        </mat-tab>

        <!-- GATES TAB -->
        <mat-tab label="Gates">
          @if (loadingGates()) {
            <div class="loading-state">
              <span class="material-symbols-outlined spin">progress_activity</span>
              Chargement...
            </div>
          } @else if (errorGates()) {
            <div class="error-panel">
              <span class="material-symbols-outlined">error</span>
              {{ errorGates() }}
            </div>
          } @else if (gates().length === 0) {
            <tch-admin-empty-state icon="toggle_off" title="Aucun gate" message="Aucun gate configuré." />
          } @else {
            <table mat-table [dataSource]="gates()">
              <ng-container matColumnDef="jobKey">
                <th mat-header-cell *matHeaderCellDef>Clé</th>
                <td mat-cell *matCellDef="let row">{{ row.jobKey }}</td>
              </ng-container>
              <ng-container matColumnDef="enabled">
                <th mat-header-cell *matHeaderCellDef>Activé</th>
                <td mat-cell *matCellDef="let row">
                  <button
                    mat-stroked-button
                    [color]="row.enabled ? 'warn' : 'primary'"
                    (click)="toggleGate(row)"
                  >
                    {{ row.enabled ? 'Désactiver' : 'Activer' }}
                  </button>
                </td>
              </ng-container>
              <ng-container matColumnDef="reason">
                <th mat-header-cell *matHeaderCellDef>Raison</th>
                <td mat-cell *matCellDef="let row">{{ row.reason ?? '—' }}</td>
              </ng-container>
              <tr mat-header-row *matHeaderRowDef="gateColumns"></tr>
              <tr mat-row *matRowDef="let row; columns: gateColumns"></tr>
            </table>
          }
        </mat-tab>

        <!-- EXECUTIONS TAB -->
        <mat-tab label="Exécutions">
          @if (loadingExecs()) {
            <div class="loading-state">
              <span class="material-symbols-outlined spin">progress_activity</span>
              Chargement...
            </div>
          } @else if (errorExecs()) {
            <div class="error-panel">
              <span class="material-symbols-outlined">error</span>
              {{ errorExecs() }}
            </div>
          } @else if (executions().length === 0) {
            <tch-admin-empty-state icon="history" title="Aucune exécution" message="Aucune exécution enregistrée." />
          } @else {
            <table mat-table [dataSource]="executions()">
              <ng-container matColumnDef="executionId">
                <th mat-header-cell *matHeaderCellDef>ID</th>
                <td mat-cell *matCellDef="let row">{{ row.executionId }}</td>
              </ng-container>
              <ng-container matColumnDef="jobKey">
                <th mat-header-cell *matHeaderCellDef>Clé</th>
                <td mat-cell *matCellDef="let row">{{ row.jobKey }}</td>
              </ng-container>
              <ng-container matColumnDef="status">
                <th mat-header-cell *matHeaderCellDef>Statut</th>
                <td mat-cell *matCellDef="let row">{{ row.status }}</td>
              </ng-container>
              <ng-container matColumnDef="startedAt">
                <th mat-header-cell *matHeaderCellDef>Début</th>
                <td mat-cell *matCellDef="let row">{{ row.startedAt }}</td>
              </ng-container>
              <ng-container matColumnDef="endedAt">
                <th mat-header-cell *matHeaderCellDef>Fin</th>
                <td mat-cell *matCellDef="let row">{{ row.endedAt ?? '—' }}</td>
              </ng-container>
              <ng-container matColumnDef="tenantCode">
                <th mat-header-cell *matHeaderCellDef>Tenant</th>
                <td mat-cell *matCellDef="let row">{{ row.tenantCode ?? '—' }}</td>
              </ng-container>
              <ng-container matColumnDef="dryRun">
                <th mat-header-cell *matHeaderCellDef>Dry-run</th>
                <td mat-cell *matCellDef="let row">{{ row.dryRun ? 'Oui' : 'Non' }}</td>
              </ng-container>
              <tr mat-header-row *matHeaderRowDef="execColumns"></tr>
              <tr mat-row *matRowDef="let row; columns: execColumns"></tr>
            </table>
          }
        </mat-tab>
      </mat-tab-group>
    </tch-admin-page-shell>
  `,
  styles: [
    `
      .loading-state {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        padding: 2rem;
        color: var(--tch-color-on-surface-variant);
      }
      .spin {
        animation: spin 0.8s linear infinite;
        display: inline-block;
      }
      @keyframes spin { to { transform: rotate(360deg); } }
      .error-panel {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        padding: 1rem;
        border-radius: 0.5rem;
        background: var(--tch-color-error-container, #ffdad6);
        color: var(--tch-color-on-error-container, #410002);
        margin: 1rem 0;
      }
      table {
        width: 100%;
      }
    `,
  ],
})
export class PlatformOpsBatchPage implements OnInit {
  private readonly api = inject(PlatformOpsApi);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly jobColumns = ['jobKey', 'status', 'lastRunAt', 'nextRunAt', 'actions'];
  readonly gateColumns = ['jobKey', 'enabled', 'reason'];
  readonly execColumns = ['executionId', 'jobKey', 'status', 'startedAt', 'endedAt', 'tenantCode', 'dryRun'];

  readonly loadingJobs = signal(false);
  readonly errorJobs = signal<string | null>(null);
  readonly jobs = signal<BatchJobView[]>([]);

  readonly loadingGates = signal(false);
  readonly errorGates = signal<string | null>(null);
  readonly gates = signal<BatchGateView[]>([]);

  readonly loadingExecs = signal(false);
  readonly errorExecs = signal<string | null>(null);
  readonly executions = signal<BatchExecutionView[]>([]);

  ngOnInit(): void {
    this.loadJobs();
    this.loadGates();
    this.loadExecs();
  }

  private loadJobs(): void {
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

  private loadGates(): void {
    this.loadingGates.set(true);
    this.api.listGates().subscribe({
      next: v => { this.gates.set(v); this.loadingGates.set(false); },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string } })?.error;
        this.errorGates.set(pd?.title ?? 'Erreur.');
        this.loadingGates.set(false);
      },
    });
  }

  private loadExecs(): void {
    this.loadingExecs.set(true);
    this.api.listExecutions().subscribe({
      next: v => { this.executions.set(v); this.loadingExecs.set(false); },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string } })?.error;
        this.errorExecs.set(pd?.title ?? 'Erreur.');
        this.loadingExecs.set(false);
      },
    });
  }

  openStartJobDialog(jobKey: JobKey): void {
    this.dialog.open(OpsStartJobDialog, { data: { jobKey }, width: '480px' });
  }

  toggleGate(gate: BatchGateView): void {
    this.api.updateGate(gate.jobKey, !gate.enabled).subscribe({
      next: () => {
        this.snackBar.open(`Gate ${gate.jobKey} ${!gate.enabled ? 'activé' : 'désactivé'}.`, 'OK', { duration: 3000 });
        this.loadGates();
      },
      error: () => this.snackBar.open('Erreur lors de la mise à jour.', 'OK', { duration: 4000 }),
    });
  }
}
