import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';

import { PlatformOpsApi } from '../../../platform-ops-api.service';

@Component({
  selector: 'tch-clear-all-caches-dialog',
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
    <h2 mat-dialog-title>Vider tous les caches</h2>
    <mat-dialog-content>
      <div class="clear-caches-dialog__warning">
        <span class="material-symbols-outlined">warning</span>
        Cette action vide tous les caches de l'application. Elle peut provoquer une dégradation
        temporaire des performances.
      </div>
      <form [formGroup]="form" class="clear-caches-dialog__form">
        <mat-form-field appearance="outline" class="clear-caches-dialog__field">
          <mat-label>Raison (min. 10 caractères)</mat-label>
          <textarea matInput formControlName="reason" rows="3"></textarea>
          @if (form.controls.reason.invalid && form.controls.reason.touched) {
            <mat-error>Raison requise (min. 10 caractères).</mat-error>
          }
        </mat-form-field>
        <mat-checkbox formControlName="confirmed">
          Je confirme vouloir vider tous les caches.
        </mat-checkbox>
      </form>

      @if (error()) {
        <div class="clear-caches-dialog__error">
          <span class="material-symbols-outlined">error</span>
          {{ error() }}
          @if (traceId()) {
            <span class="clear-caches-dialog__trace">ID: {{ traceId() }}</span>
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
        Vider tout
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .clear-caches-dialog__warning {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.75rem;
      border-radius: var(--tch-radius-sm, 0.5rem);
      background: var(--tch-color-warning-container);
      color: var(--tch-color-on-warning-container);
      font-size: 0.875rem;
      margin-bottom: 1rem;
    }
    .clear-caches-dialog__form {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
      width: 100%;
    }
    .clear-caches-dialog__field { width: 100%; }
    .clear-caches-dialog__error {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.75rem;
      border-radius: var(--tch-radius-sm, 0.5rem);
      background: var(--tch-color-error-container);
      color: var(--tch-color-on-error-container);
      font-size: 0.875rem;
      margin-top: 0.5rem;
    }
    .clear-caches-dialog__trace { font-size: 0.75rem; opacity: 0.7; margin-left: 0.25rem; }
    .spin { animation: spin 0.8s linear infinite; display: inline-block; vertical-align: middle; }
    @keyframes spin { to { transform: rotate(360deg); } }
  `],
})
export class ClearAllCachesDialog {
  private readonly dialogRef = inject(MatDialogRef<ClearAllCachesDialog>);
  private readonly api = inject(PlatformOpsApi);
  private readonly snackBar = inject(MatSnackBar);
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

    this.api.clearAllCaches(this.form.controls.reason.value!).subscribe({
      next: () => {
        this.submitting.set(false);
        this.snackBar.open('Tous les caches ont été vidés.', 'OK', { duration: 4000 });
        this.dialogRef.close(true);
      },
      error: (err: unknown) => {
        this.submitting.set(false);
        const pd = (err as { error?: { title?: string; errorId?: string; requestId?: string } })?.error;
        this.error.set(pd?.title ?? "Erreur lors de l'opération.");
        this.traceId.set(pd?.errorId ?? pd?.requestId ?? null);
      },
    });
  }
}
