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
      <div class="start-job-dialog__usage">
        <span class="material-symbols-outlined" aria-hidden="true">info</span>
        <span>{{ usageHint(data.job.job_key) }}</span>
      </div>
      @if (data.job.required_params.length) {
        <div class="start-job-dialog__params">
          <strong>Requis</strong>
          @for (param of data.job.required_params; track param) {
            <code>{{ param }}</code>
          }
        </div>
      }
      @if (data.job.optional_params.length) {
        <div class="start-job-dialog__params">
          <strong>Optionnels</strong>
          @for (param of data.job.optional_params; track param) {
            <code>{{ param }}</code>
          }
        </div>
      }
      <form [formGroup]="form" class="start-job-dialog__form">
        <mat-form-field appearance="outline">
          <mat-label>Paramètres JSON à ajuster</mat-label>
          <textarea matInput formControlName="paramsJson" rows="8" spellcheck="false"></textarea>
          @if (form.controls.paramsJson.invalid && form.controls.paramsJson.touched) {
            <mat-error>JSON invalide.</mat-error>
          }
        </mat-form-field>
        <div class="start-job-dialog__tools">
          <button mat-button type="button" (click)="resetExample()">
            <span class="material-symbols-outlined" aria-hidden="true">restart_alt</span>
            Remettre l'exemple
          </button>
          <button mat-button type="button" (click)="formatJson()">
            <span class="material-symbols-outlined" aria-hidden="true">data_object</span>
            Formatter
          </button>
        </div>
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
      <button mat-button (click)="close()">{{ result() ? 'Fermer' : 'Annuler' }}</button>
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
    .start-job-dialog__usage { display: flex; gap: 0.5rem; align-items: flex-start; margin-bottom: 0.75rem; color: var(--tch-color-on-surface-variant); font-size: var(--tch-font-size-body-sm); }
    .start-job-dialog__usage .material-symbols-outlined { font-size: 1.125rem; color: var(--tch-color-primary); }
    .start-job-dialog__params { display: flex; flex-wrap: wrap; gap: 0.375rem; align-items: center; margin-bottom: 0.5rem; font-size: var(--tch-font-size-label-md); }
    .start-job-dialog__params strong { margin-right: 0.25rem; color: var(--tch-color-on-surface); }
    .start-job-dialog__params code { padding: 0.125rem 0.375rem; border-radius: var(--tch-radius-pill); background: var(--tch-color-surface-container-high); color: var(--tch-color-on-surface-variant); font-family: monospace; font-size: var(--tch-font-size-label-sm); }
    .start-job-dialog__form { display: flex; flex-direction: column; gap: 0.75rem; width: 100%; }
    .start-job-dialog__tools { display: flex; flex-wrap: wrap; gap: 0.5rem; justify-content: flex-end; margin-top: -0.5rem; }
    .start-job-dialog__tools button { display: inline-flex; align-items: center; gap: 0.25rem; }
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
    paramsJson: [jobExampleJson(this.data.job)],
  });

  submit(): void {
    if (this.submitting() || this.result()) return;
    let params: Record<string, string>;
    try {
      params = normalizeJobParams(JSON.parse(this.form.value.paramsJson ?? '{}'));
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

  close(): void {
    this.dialogRef.close(this.result());
  }

  resetExample(): void {
    this.form.controls.paramsJson.setValue(jobExampleJson(this.data.job));
    this.form.controls.paramsJson.setErrors(null);
  }

  formatJson(): void {
    try {
      const params = normalizeJobParams(JSON.parse(this.form.value.paramsJson ?? '{}'));
      this.form.controls.paramsJson.setValue(JSON.stringify(params, null, 2));
      this.form.controls.paramsJson.setErrors(null);
    } catch {
      this.form.controls.paramsJson.setErrors({ invalidJson: true });
      this.form.markAllAsTouched();
    }
  }

  usageHint(jobKey: string): string {
    return JOB_USAGE_HINTS[jobKey] ?? 'Execution technique Spring Batch. Utilisez les pages metier Tirages/Resultats pour les actions guidees.';
  }
}

const EXAMPLE_TENANT_ID = '00000000-0000-0000-0000-000000000003';

const JOB_USAGE_HINTS: Record<string, string> = {
  'draw:lifecycle:generate': 'Genere des tirages pour un tenant. Pour une action guidee multi-tenant/date, preferez Operations > Tirages > Generer.',
  'draw:lifecycle:open': 'Ouvre les tirages d un tenant dans une fenetre. Pour le quotidien, preferez Operations > Tirages > Ouvrir.',
  'draw:lifecycle:close': 'Ferme les tirages d un tenant dans une fenetre. Pour le quotidien, preferez Operations > Tirages > Fermer.',
  'draw:lifecycle:settle': 'Regle les tirages ayant des resultats. Lancez d abord en dry_run pour verifier le volume.',
  'results:external:fetch': 'Recupere les resultats externes sans tenant. Pour une action simple, preferez Operations > Resultats > Fetch.',
  'results:external:apply': 'Applique des resultats fetches aux tirages d un tenant. Pour une action guidee, preferez Operations > Resultats > Refresh.',
  'catalog:search:reindex': 'Reconstruit l index de recherche catalogue. Action globale technique, a utiliser ponctuellement.',
};

function jobExampleJson(job: JobInfoResponse): string {
  return JSON.stringify(jobExample(job), null, 2);
}

function jobExample(job: JobInfoResponse): Record<string, string> {
  const base: Record<string, string> = {};
  if (job.required_params.includes('tenant_id')) {
    base['tenant_id'] = EXAMPLE_TENANT_ID;
  }

  switch (job.job_key) {
    case 'draw:lifecycle:generate':
      return { ...base, days_ahead: '7', dry_run: 'true' };
    case 'draw:lifecycle:open':
    case 'draw:lifecycle:close':
      return {
        ...base,
        from: `${todayIsoDate()}T00:00:00Z`,
        to: `${todayIsoDate()}T23:59:59Z`,
      };
    case 'draw:lifecycle:settle':
      return { ...base, date: todayIsoDate(), days_back: '1', max_draws: '100', dry_run: 'true' };
    case 'results:external:fetch':
      return { date: todayIsoDate(), slot_key: 'FL_EVE', max_slots: '20', dry_run: 'true' };
    case 'results:external:apply':
      return { ...base, date: todayIsoDate(), slot_key: 'FL_EVE', days_back: '1', max_slots: '20', dry_run: 'true' };
    case 'catalog:search:reindex':
      return { full_rebuild: 'true', max_items: '1000' };
    default:
      for (const param of job.required_params) base[param] = param === 'tenant_id' ? EXAMPLE_TENANT_ID : '';
      return base;
  }
}

function normalizeJobParams(value: unknown): Record<string, string> {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new Error('job params must be an object');
  }
  return Object.fromEntries(
    Object.entries(value as Record<string, unknown>)
      .filter(([, v]) => v !== undefined && v !== null)
      .map(([k, v]) => [k, typeof v === 'string' ? v : String(v)]),
  );
}

function todayIsoDate(): string {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}
