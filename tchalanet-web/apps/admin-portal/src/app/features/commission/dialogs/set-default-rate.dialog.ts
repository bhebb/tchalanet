import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import {
  mapHttpErrorToProblemDetail,
  webAppErrorFromProblemDetail,
  webAppErrorsFromProblemDetailFields,
} from '@tch/api';
import {
  AdminFormActions,
  TchErrorPanel,
  TchFieldError,
  TchFormErrorSummary,
} from '@tch/ui/components';
import {
  applyServerFieldErrors,
  clearServerFieldErrors,
  clearServerFieldErrorsOnEdit,
  ErrorViewModel,
  resolveErrorFeedbackCopy,
  toErrorViewModel,
  withResolvedErrorCopies,
} from '@tch/web/errors';

import { AdminCommissionApi } from '../data-access/admin-commission-api.service';

const DEFAULT_RATE_FIELD_TARGETS = {
  rate: 'rate',
  'admin.commission.defaultRate.rate': 'rate',
} as const;

@Component({
  selector: 'tch-set-default-rate-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    AdminFormActions,
    TranslatePipe,
    TchErrorPanel,
    TchFieldError,
    TchFormErrorSummary,
  ],
  templateUrl: './set-default-rate.dialog.html',
  styleUrls: ['./set-default-rate.dialog.scss'],
})
export class SetDefaultRateDialog {
  protected readonly data = inject<{ current: number }>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<SetDefaultRateDialog, boolean>);
  private readonly api = inject(AdminCommissionApi);
  private readonly fb = inject(FormBuilder);
  private readonly translate = inject(TranslateService);
  private readonly destroyRef = inject(DestroyRef);

  readonly saving = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly formSummary = signal<readonly string[]>([]);

  readonly form = this.fb.group({
    rate: [this.data.current, [Validators.required, Validators.min(0), Validators.max(100)]],
  });

  constructor() {
    const cleanup = clearServerFieldErrorsOnEdit(this.form);
    this.destroyRef.onDestroy(() => cleanup.unsubscribe());
  }

  submit(): void {
    if (this.saving()) return;
    clearServerFieldErrors(this.form);
    this.error.set(null);
    this.formSummary.set([]);
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const rate = this.form.controls.rate.value;
    if (rate == null) return;

    this.saving.set(true);
    this.api.setDefaultRate(rate, { suppressShellFeedback: true }).subscribe({
      next: () => this.dialogRef.close(true),
      error: (err: unknown) => {
        this.handleSubmitError(err);
        this.saving.set(false);
      },
    });
  }

  cancel(): void {
    if (!this.saving()) this.dialogRef.close(false);
  }

  private handleSubmitError(err: unknown): void {
    const problem = mapHttpErrorToProblemDetail(err);
    const fieldErrors = withResolvedErrorCopies(
      webAppErrorsFromProblemDetailFields(problem, 'admin.commission.defaultRate'),
      key => this.translate.instant(key),
    );
    const remaining = applyServerFieldErrors(this.form, fieldErrors, DEFAULT_RATE_FIELD_TARGETS);
    if (fieldErrors.length && remaining.length) {
      this.formSummary.set(remaining.flatMap(item => (item.message ? [item.message] : [])));
      return;
    }
    if (fieldErrors.length) return;

    const normalized = webAppErrorFromProblemDetail(
      problem,
      'admin.commission.defaultRate',
      'form',
    );
    const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
    this.error.set(toErrorViewModel(normalized, copy));
  }
}
