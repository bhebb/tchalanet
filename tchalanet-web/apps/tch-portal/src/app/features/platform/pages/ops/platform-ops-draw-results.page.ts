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

import { AdminPageShellComponent } from '../../../private/shared/admin-ui/admin-page-shell.component';
import { AdminEmptyStateComponent } from '../../../private/shared/admin-ui/admin-empty-state.component';
import { PlatformOpsApi, DrawResultView, DrawOperationRequest } from '../../platform-ops-api.service';

// ── Confirm action dialog (for sensitive per-row ops) ──────────────────────────

@Component({
  selector: 'tch-draw-result-action-dialog',
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
    <h2 mat-dialog-title>{{ data.title }}</h2>
    <mat-dialog-content>
      <div class="audit-warning">
        <span class="material-symbols-outlined">warning</span>
        Cette action est sensible et sera enregistrée dans l'audit. Assurez-vous d'avoir les droits
        nécessaires.
      </div>

      <form [formGroup]="form" class="dialog-form">
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Raison (min. 10 caractères)</mat-label>
          <textarea matInput formControlName="reason" rows="3"></textarea>
          @if (form.controls.reason.invalid && form.controls.reason.touched) {
            <mat-error>Raison requise (min. 10 caractères).</mat-error>
          }
        </mat-form-field>

        <mat-checkbox formControlName="confirmed">
          Je confirme cette action sensible.
        </mat-checkbox>
      </form>

      @if (error()) {
        <div class="error-panel">
          <span class="material-symbols-outlined">error</span>
          {{ error() }}
          @if (traceId()) {
            <span class="trace-id">ID: {{ traceId() }}</span>
          }
        </div>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close [disabled]="submitting()">Annuler</button>
      <button
        mat-flat-button
        color="warn"
        [disabled]="form.invalid || submitting()"
        (click)="submit()"
      >
        @if (submitting()) {
          <span class="material-symbols-outlined spin">progress_activity</span>
        }
        Confirmer
      </button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      .audit-warning {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        padding: 0.75rem;
        border-radius: 0.5rem;
        background: var(--tch-color-warning-container, #fff3cd);
        color: var(--tch-color-on-warning-container, #92400e);
        font-size: 0.875rem;
        margin-bottom: 1rem;
      }
      .dialog-form { display: flex; flex-direction: column; gap: 0.75rem; min-width: 400px; }
      .full-width { width: 100%; }
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
      .trace-id { font-size: 0.75rem; opacity: 0.7; margin-left: 0.25rem; }
      .spin { animation: spin 0.8s linear infinite; display: inline-block; vertical-align: middle; }
      @keyframes spin { to { transform: rotate(360deg); } }
    `,
  ],
})
export class DrawResultActionDialog {
  protected readonly data = inject<{ title: string; onSubmit: (reason: string) => void }>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<DrawResultActionDialog>);
  private readonly fb = inject(FormBuilder);

  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  readonly traceId = signal<string | null>(null);

  readonly form = this.fb.group({
    reason: ['', [Validators.required, Validators.minLength(10)]],
    confirmed: [false, Validators.requiredTrue],
  });

  submit(): void {
    if (this.form.invalid || this.submitting()) return;
    this.submitting.set(true);
    this.error.set(null);
    // Delegate to the caller's handler
    this.data.onSubmit(this.form.controls.reason.value!);
    // The caller is responsible for closing/erroring; we just pass control
    this.dialogRef.close(true);
  }
}

// ── Main page ──────────────────────────────────────────────────────────────────

@Component({
  selector: 'tch-platform-ops-draw-results-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminPageShellComponent,
    AdminEmptyStateComponent,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
  ],
  template: `
    <tch-admin-page-shell
      title="Résultats des tirages"
      description="Gestion et confirmation des résultats externes."
    >
      <div actions>
        <button mat-stroked-button [disabled]="actionLoading()" (click)="fetchResults()">
          <span class="material-symbols-outlined">download</span>
          Fetch
        </button>
        <button mat-stroked-button [disabled]="actionLoading()" (click)="refreshResults()">
          <span class="material-symbols-outlined">refresh</span>
          Refresh
        </button>
      </div>

      @if (actionError()) {
        <div class="error-panel">
          <span class="material-symbols-outlined">error</span>
          {{ actionError() }}
          @if (actionTraceId()) {
            <span class="trace-id">ID: {{ actionTraceId() }}</span>
          }
        </div>
      }

      @if (loading()) {
        <div class="loading-state">
          <span class="material-symbols-outlined spin">progress_activity</span>
          Chargement...
        </div>
      } @else if (error()) {
        <div class="error-panel">
          <span class="material-symbols-outlined">error</span>
          {{ error() }}
          @if (traceId()) {
            <span class="trace-id">ID: {{ traceId() }}</span>
          }
        </div>
      } @else if (results().length === 0) {
        <tch-admin-empty-state
          icon="receipt_long"
          title="Aucun résultat"
          message="Aucun résultat de tirage disponible."
        />
      } @else {
        <div class="table-container">
          <table mat-table [dataSource]="results()">
            <ng-container matColumnDef="drawResultId">
              <th mat-header-cell *matHeaderCellDef>ID Résultat</th>
              <td mat-cell *matCellDef="let row">{{ row.drawResultId }}</td>
            </ng-container>
            <ng-container matColumnDef="drawId">
              <th mat-header-cell *matHeaderCellDef>ID Tirage</th>
              <td mat-cell *matCellDef="let row">{{ row.drawId }}</td>
            </ng-container>
            <ng-container matColumnDef="slotCode">
              <th mat-header-cell *matHeaderCellDef>Slot</th>
              <td mat-cell *matCellDef="let row">{{ row.slotCode }}</td>
            </ng-container>
            <ng-container matColumnDef="status">
              <th mat-header-cell *matHeaderCellDef>Statut</th>
              <td mat-cell *matCellDef="let row">{{ row.status }}</td>
            </ng-container>
            <ng-container matColumnDef="fetchedAt">
              <th mat-header-cell *matHeaderCellDef>Récupéré le</th>
              <td mat-cell *matCellDef="let row">{{ row.fetchedAt ?? '—' }}</td>
            </ng-container>
            <ng-container matColumnDef="confirmedAt">
              <th mat-header-cell *matHeaderCellDef>Confirmé le</th>
              <td mat-cell *matCellDef="let row">{{ row.confirmedAt ?? '—' }}</td>
            </ng-container>
            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef></th>
              <td mat-cell *matCellDef="let row">
                <div class="row-actions">
                  @if (!row.confirmedAt) {
                    <button mat-stroked-button (click)="confirmResult(row)">
                      <span class="material-symbols-outlined">check_circle</span>
                      Confirmer
                    </button>
                  }
                </div>
              </td>
            </ng-container>
            <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedColumns"></tr>
          </table>
        </div>
      }
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
      .spin { animation: spin 0.8s linear infinite; display: inline-block; }
      @keyframes spin { to { transform: rotate(360deg); } }
      .error-panel {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        padding: 1rem;
        border-radius: 0.5rem;
        background: var(--tch-color-error-container, #ffdad6);
        color: var(--tch-color-on-error-container, #410002);
        margin-bottom: 1rem;
      }
      .trace-id { font-size: 0.75rem; opacity: 0.7; }
      .table-container { overflow-x: auto; }
      table { width: 100%; }
      .row-actions { display: flex; gap: 0.5rem; }
    `,
  ],
})
export class PlatformOpsDrawResultsPage implements OnInit {
  private readonly api = inject(PlatformOpsApi);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly displayedColumns = ['drawResultId', 'drawId', 'slotCode', 'status', 'fetchedAt', 'confirmedAt', 'actions'];
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly traceId = signal<string | null>(null);
  readonly results = signal<DrawResultView[]>([]);
  readonly actionLoading = signal(false);
  readonly actionError = signal<string | null>(null);
  readonly actionTraceId = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.listDrawResults().subscribe({
      next: v => { this.results.set(v); this.loading.set(false); },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string; errorId?: string; requestId?: string } })?.error;
        this.error.set(pd?.title ?? 'Erreur de chargement.');
        this.traceId.set(pd?.errorId ?? pd?.requestId ?? null);
        this.loading.set(false);
      },
    });
  }

  fetchResults(): void {
    this.actionLoading.set(true);
    this.actionError.set(null);
    this.api.fetchDrawResults({ dryRun: false }).subscribe({
      next: () => {
        this.actionLoading.set(false);
        this.snackBar.open('Résultats récupérés.', 'OK', { duration: 3000 });
        this.load();
      },
      error: (err: unknown) => {
        this.actionLoading.set(false);
        const pd = (err as { error?: { title?: string; errorId?: string; requestId?: string } })?.error;
        this.actionError.set(pd?.title ?? 'Erreur.');
        this.actionTraceId.set(pd?.errorId ?? pd?.requestId ?? null);
      },
    });
  }

  refreshResults(): void {
    this.actionLoading.set(true);
    this.actionError.set(null);
    this.api.refreshDrawResults({ dryRun: false }).subscribe({
      next: () => {
        this.actionLoading.set(false);
        this.snackBar.open('Résultats rafraîchis.', 'OK', { duration: 3000 });
        this.load();
      },
      error: (err: unknown) => {
        this.actionLoading.set(false);
        const pd = (err as { error?: { title?: string; errorId?: string; requestId?: string } })?.error;
        this.actionError.set(pd?.title ?? 'Erreur.');
        this.actionTraceId.set(pd?.errorId ?? pd?.requestId ?? null);
      },
    });
  }

  confirmResult(row: DrawResultView): void {
    this.dialog.open(DrawResultActionDialog, {
      data: {
        title: `Confirmer le résultat — ${row.slotCode}`,
        onSubmit: (reason: string) => {
          this.api.confirmDrawResult(row.drawResultId, reason).subscribe({
            next: () => {
              this.snackBar.open('Résultat confirmé.', 'OK', { duration: 3000 });
              this.load();
            },
            error: (err: unknown) => {
              const pd = (err as { error?: { title?: string; errorId?: string } })?.error;
              this.snackBar.open(pd?.title ?? 'Erreur de confirmation.', 'OK', { duration: 5000 });
            },
          });
        },
      },
      width: '480px',
    });
  }
}
