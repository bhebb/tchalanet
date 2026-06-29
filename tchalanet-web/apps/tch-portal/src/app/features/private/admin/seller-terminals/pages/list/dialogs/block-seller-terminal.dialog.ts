import {
  ChangeDetectionStrategy,
  Component,
  inject,
  signal,
} from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { TranslateService } from '@ngx-translate/core';
import { ProblemDetail, webAppErrorFromProblemDetail, webAppErrorsFromProblemDetailFields } from '@tch/api';

import { TchErrorPanel, TchFieldError } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '../../../../../../../core/api/error-feedback-copy';
import {
  applyServerFieldErrors,
  clearServerFieldErrors,
  ErrorViewModel,
  toErrorViewModel,
  withResolvedErrorCopies,
} from '../../../../../../../core/api/local-error-routing';
import { SellerTerminalApi, SellerTerminalSummaryRow } from '../../../../seller-terminal-api.service';

const BLOCK_FIELD_TARGETS = {
  reason: 'reason',
  'sellerTerminal.block.reason': 'reason',
  'admin.sellerTerminal.block.reason': 'reason',
} as const;

@Component({
  selector: 'tch-block-seller-terminal-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    TchErrorPanel,
    TchFieldError,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  templateUrl: './block-seller-terminal.dialog.html',
  styleUrls: ['./block-seller-terminal.dialog.scss'],
})
export class BlockSellerTerminalDialog {
  protected readonly data = inject<SellerTerminalSummaryRow>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<BlockSellerTerminalDialog>);
  private readonly api = inject(SellerTerminalApi);
  private readonly fb = inject(FormBuilder);
  private readonly translate = inject(TranslateService);

  readonly saving = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);

  readonly form = this.fb.nonNullable.group({
    reason: ['', [Validators.required, Validators.maxLength(200)]],
  });

  submit(): void {
    if (this.saving()) return;
    clearServerFieldErrors(this.form);
    this.error.set(null);
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    this.saving.set(true);
    this.api.block(this.data.id.value, this.form.controls.reason.value, { suppressShellFeedback: true }).subscribe({
      next: () => this.dialogRef.close({ reload: true }),
      error: (err: unknown) => {
        this.handleSubmitError(err);
        this.saving.set(false);
      },
    });
  }

  serverFieldMessage(control: AbstractControl | null): string {
    const server = control?.errors?.['server'];
    return typeof server === 'object' &&
      server !== null &&
      'message' in server &&
      typeof (server as { message?: unknown }).message === 'string'
      ? (server as { message: string }).message
      : '';
  }

  private handleSubmitError(err: unknown): void {
    const problem = (err as { error?: ProblemDetail })?.error;
    if (problem) {
      const fieldErrors = withResolvedErrorCopies(
        webAppErrorsFromProblemDetailFields(problem, 'admin.sellerTerminal.block'),
        key => this.translate.instant(key),
      );
      const remaining = applyServerFieldErrors(this.form, fieldErrors, BLOCK_FIELD_TARGETS);
      if (fieldErrors.length && !remaining.length) return;

      const normalized = webAppErrorFromProblemDetail(problem, 'admin.sellerTerminal.block', 'page');
      const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
      this.error.set(toErrorViewModel(normalized, copy));
      return;
    }

    this.error.set({
      title: this.translate.instant('common.errors.fallback.title'),
      message: this.translate.instant('common.errors.fallback.message'),
      severity: 'error',
    });
  }
}
