import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { TranslateService } from '@ngx-translate/core';

import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { TchSectionError } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '../../../../../../core/api/error-feedback-copy';
import { ErrorViewModel, toErrorViewModel } from '../../../../../../core/api/local-error-routing';
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
    TchSectionError,
  ],
  templateUrl: './start-job.dialog.html',
  styleUrls: ['./start-job.dialog.scss'],
})
export class StartJobDialog {
  protected readonly data = inject<{ job: JobInfoResponse }>(MAT_DIALOG_DATA);
  readonly dialogRef = inject(MatDialogRef<StartJobDialog>);
  private readonly api = inject(PlatformOpsApi);
  private readonly fb = inject(FormBuilder);
  private readonly translate = inject(TranslateService);

  readonly submitting = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
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
    this.api.startJob(this.data.job.job_key, req, { suppressShellFeedback: true }).subscribe({
      next: (res) => { this.submitting.set(false); this.result.set(res); },
      error: (err: unknown) => {
        this.submitting.set(false);
        this.error.set(this.errorViewModel(err, 'platform.ops.batch.start'));
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

  private errorViewModel(err: unknown, source: string): ErrorViewModel {
    const problem = (err as { error?: ProblemDetail })?.error;
    if (problem) {
      const normalized = webAppErrorFromProblemDetail(problem, source, 'page');
      const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
      return toErrorViewModel(normalized, copy);
    }

    return {
      title: this.translate.instant('common.errors.fallback.title'),
      message: this.translate.instant('common.errors.fallback.message'),
      severity: 'error',
    };
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
