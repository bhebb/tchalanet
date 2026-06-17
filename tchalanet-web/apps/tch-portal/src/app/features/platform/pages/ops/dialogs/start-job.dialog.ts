import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

import {
  PlatformOpsApi,
  JobInfoResponse,
  StartJobRequest,
  StartJobResponse,
} from '../../../platform-ops-api.service';

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
      <p class="start-job-dialog__job-key"><strong>{{ data.job.job_key }}</strong> — {{ data.job.display_name }}</p>
      @if (data.job.required_params?.length) {
        <p class="start-job-dialog__hint">Paramètres requis: {{ data.job.required_params.join(', ') }}</p>
      }
      @if (data.job.optional_params?.length) {
        <p class="start-job-dialog__hint">Paramètres optionnels: {{ data.job.optional_params.join(', ') }}</p>
      }
      <form [formGroup]="form" class="start-job-dialog__form">
        <mat-form-field appearance="outline">
          <mat-label>Paramètres JSON (clés snake_case)</mat-label>
          <textarea matInput formControlName="paramsJson" rows="4" placeholder='{}'></textarea>
          @if (form.controls.paramsJson.invalid && form.controls.paramsJson.touched) {
            <mat-error>JSON invalide.</mat-error>
          }
        </mat-form-field>
      </form>
      @if (result()) {
        <div class="start-job-dialog__result">
          Job démarré — Exécution #{{ result()!.execution_id }} · Statut: {{ result()!.status }}
        </div>
      }
      @if (error()) {
        <div class="start-job-dialog__error">{{ error() }}</div>
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
    .start-job-dialog__job-key { background: var(--tch-color-surface-container); padding: 0.5rem 0.75rem; border-radius: var(--tch-radius-sm); margin-bottom: 0.5rem; font-size: 0.875rem; }
    .start-job-dialog__hint { font-size: 0.8125rem; color: var(--tch-color-on-surface-variant); margin: 0 0 0.5rem; }
    .start-job-dialog__form { display: flex; flex-direction: column; gap: 0.75rem; width: 100%; }
    .start-job-dialog__result { background: var(--tch-color-success-container); color: var(--tch-color-on-success-container); padding: 0.75rem; border-radius: var(--tch-radius-sm); font-size: 0.875rem; margin-top: 0.75rem; }
    .start-job-dialog__error { background: var(--tch-color-error-container); color: var(--tch-color-on-error-container); padding: 0.75rem; border-radius: var(--tch-radius-sm); font-size: 0.875rem; margin-top: 0.5rem; }
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
