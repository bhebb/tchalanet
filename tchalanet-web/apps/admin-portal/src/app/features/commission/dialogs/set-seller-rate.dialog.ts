import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { finalize } from 'rxjs';
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
  withResolvedErrorCopy,
  withResolvedErrorCopies,
} from '@tch/web/errors';

import {
  AdminCommissionApi,
  SellerTerminalCommissionRow,
  sellerTerminalCommissionId,
} from '../data-access/admin-commission-api.service';
import { commissionFieldForCode, COMMISSION_FIELD_TARGETS } from '../commission-error-targets';

@Component({
  selector: 'tch-set-seller-rate-dialog',
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
  templateUrl: './set-seller-rate.dialog.html',
  styleUrls: ['./set-seller-rate.dialog.scss'],
})
export class SetSellerRateDialog {
  protected readonly data = inject<{ row: SellerTerminalCommissionRow }>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<SetSellerRateDialog, boolean>);
  private readonly api = inject(AdminCommissionApi);
  private readonly fb = inject(FormBuilder);
  private readonly translate = inject(TranslateService);
  private readonly destroyRef = inject(DestroyRef);

  readonly saving = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly formSummary = signal<readonly string[]>([]);

  readonly form = this.fb.group({
    rate: [
      this.data.row.commissionRate,
      [Validators.required, Validators.min(0), Validators.max(100)],
    ],
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

    const sellerTerminalId = sellerTerminalCommissionId(this.data.row);
    this.saving.set(true);
    this.api
      .setSellerRate(sellerTerminalId, rate, { suppressShellFeedback: true })
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: () => this.dialogRef.close(true),
        error: (err: unknown) => this.handleSubmitError(err),
      });
  }

  cancel(): void {
    if (!this.saving()) this.dialogRef.close(false);
  }

  private handleSubmitError(err: unknown): void {
    const problem = mapHttpErrorToProblemDetail(err);
    const fieldErrors = withResolvedErrorCopies(
      webAppErrorsFromProblemDetailFields(problem, 'admin.commission.sellerRate'),
      key => this.translate.instant(key),
    );
    const remaining = applyServerFieldErrors(this.form, fieldErrors, COMMISSION_FIELD_TARGETS);
    if (fieldErrors.length && remaining.length) {
      this.formSummary.set(remaining.flatMap(item => (item.message ? [item.message] : [])));
      return;
    }
    if (fieldErrors.length) return;

    if (commissionFieldForCode(problem.code) === 'rate') {
      const normalized = withResolvedErrorCopy(
        webAppErrorFromProblemDetail(problem, 'admin.commission.sellerRate', 'field'),
        key => this.translate.instant(key),
      );
      const control = this.form.controls.rate;
      control.setErrors({ ...(control.errors ?? {}), server: [normalized] });
      control.markAsTouched();
      return;
    }

    const normalized = webAppErrorFromProblemDetail(problem, 'admin.commission.sellerRate', 'page');
    const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
    this.error.set(toErrorViewModel(normalized, copy));
  }
}
