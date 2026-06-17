import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialog, MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable } from 'rxjs';

import { AdminPageShellComponent } from '../../../private/shared/admin-ui/admin-page-shell.component';
import { PlatformOpsApi, DrawOperationRequest } from '../../platform-ops-api.service';

interface DrawAction {
  id: string;
  title: string;
  description: string;
  apiCall: (req: DrawOperationRequest) => Observable<void>;
}

// ── Inline confirmation dialog ─────────────────────────────────────────────────

@Component({
  selector: 'tch-draw-op-confirm-dialog',
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
      <form [formGroup]="form" class="dialog-form">
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
      </form>

      @if (result()) {
        <div class="result-panel" [class.error]="result()!.error">
          <span class="material-symbols-outlined">
            {{ result()!.error ? 'error' : 'check_circle' }}
          </span>
          {{ result()!.message }}
          @if (result()!.traceId) {
            <span class="trace-id">ID: {{ result()!.traceId }}</span>
          }
        </div>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close [disabled]="submitting()">
        {{ result() ? 'Fermer' : 'Annuler' }}
      </button>
      @if (!result()) {
        <button
          mat-flat-button
          color="primary"
          [disabled]="form.invalid || submitting()"
          (click)="submit()"
        >
          @if (submitting()) {
            <span class="material-symbols-outlined spin">progress_activity</span>
          }
          Exécuter
        </button>
      }
    </mat-dialog-actions>
  `,
  styles: [
    `
      .dialog-form {
        display: flex;
        flex-direction: column;
        gap: 0.75rem;
        min-width: 380px;
      }
      .full-width { width: 100%; }
      .result-panel {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        padding: 0.75rem;
        border-radius: 0.5rem;
        background: #d4edda;
        color: #155724;
        font-size: 0.875rem;
        margin-top: 0.75rem;
      }
      .result-panel.error {
        background: var(--tch-color-error-container, #ffdad6);
        color: var(--tch-color-on-error-container, #410002);
      }
      .trace-id { font-size: 0.75rem; opacity: 0.7; margin-left: 0.25rem; }
      .spin {
        animation: spin 0.8s linear infinite;
        display: inline-block;
        vertical-align: middle;
      }
      @keyframes spin { to { transform: rotate(360deg); } }
    `,
  ],
})
export class DrawOpConfirmDialog {
  protected readonly data = inject<{ title: string; apiCall: (req: DrawOperationRequest) => Observable<void> }>(MAT_DIALOG_DATA);
  private readonly fb = inject(FormBuilder);

  readonly submitting = signal(false);
  readonly result = signal<{ message: string; error: boolean; traceId?: string | null } | null>(null);

  readonly form = this.fb.group({
    dryRun: [true],
    force: [false],
    reason: [''],
  });

  submit(): void {
    if (this.submitting() || this.result()) return;

    const v = this.form.value;
    if (v.force && !v.reason) {
      this.form.controls.reason.setValidators([Validators.required, Validators.minLength(1)]);
      this.form.controls.reason.updateValueAndValidity();
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);

    const req: DrawOperationRequest = {
      dryRun: v.dryRun ?? true,
      force: v.force ?? false,
      reason: v.reason || undefined,
    };

    this.data.apiCall(req).subscribe({
      next: () => {
        this.submitting.set(false);
        this.result.set({ message: 'Opération exécutée avec succès.', error: false });
      },
      error: (err: unknown) => {
        this.submitting.set(false);
        const pd = (err as { error?: { title?: string; errorId?: string; requestId?: string } })?.error;
        this.result.set({
          message: pd?.title ?? 'Erreur lors de l\'exécution.',
          error: true,
          traceId: pd?.errorId ?? pd?.requestId ?? null,
        });
      },
    });
  }
}

// ── Main page ─────────────────────────────────────────────────────────────────

@Component({
  selector: 'tch-platform-ops-draws-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminPageShellComponent,
    MatButtonModule,
    MatIconModule,
  ],
  template: `
    <tch-admin-page-shell
      title="Opérations — Tirages"
      description="Gestion manuelle du cycle de vie des tirages."
    >
      <div class="actions-grid">
        @for (action of drawActions; track action.id) {
          <div class="action-card">
            <h3 class="action-title">{{ action.title }}</h3>
            <p class="action-description">{{ action.description }}</p>
            <button mat-flat-button color="primary" (click)="openConfirm(action)">
              <span class="material-symbols-outlined">play_arrow</span>
              Exécuter
            </button>
          </div>
        }
      </div>
    </tch-admin-page-shell>
  `,
  styles: [
    `
      .actions-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
        gap: 1.25rem;
        margin-top: 1rem;
      }
      .action-card {
        border: 1px solid var(--tch-color-outline-variant);
        border-radius: 0.75rem;
        padding: 1.25rem;
        display: flex;
        flex-direction: column;
        gap: 0.75rem;
      }
      .action-title {
        margin: 0;
        font-size: 1rem;
        font-weight: 600;
      }
      .action-description {
        margin: 0;
        font-size: 0.875rem;
        color: var(--tch-color-on-surface-variant);
        flex: 1;
      }
    `,
  ],
})
export class PlatformOpsDrawsPage {
  private readonly api = inject(PlatformOpsApi);
  private readonly dialog = inject(MatDialog);

  readonly drawActions: DrawAction[] = [
    {
      id: 'generate',
      title: 'Générer les tirages',
      description: 'Crée les tirages pour la période en attente de génération.',
      apiCall: (req) => this.api.generateDraws(req),
    },
    {
      id: 'open-today',
      title: "Ouvrir les tirages du jour",
      description: 'Ouvre tous les tirages dont la date est aujourd\'hui.',
      apiCall: (req) => this.api.openTodayDraws(req),
    },
    {
      id: 'close-due',
      title: 'Fermer les tirages échus',
      description: 'Ferme tous les tirages dont l\'heure de clôture est dépassée.',
      apiCall: (req) => this.api.closeDueDraws(req),
    },
    {
      id: 'apply',
      title: 'Appliquer les résultats',
      description: 'Applique les résultats confirmés et règle les tickets gagnants.',
      apiCall: (req) => this.api.applyDrawResults(req),
    },
  ];

  openConfirm(action: DrawAction): void {
    this.dialog.open(DrawOpConfirmDialog, {
      data: { title: action.title, apiCall: action.apiCall },
      width: '460px',
    });
  }
}
