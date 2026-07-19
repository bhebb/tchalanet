import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import {
  mapHttpErrorToProblemDetail,
  webAppErrorFromProblemDetail,
  webAppErrorsFromProblemDetailFields,
} from '@tch/api';

import { TchErrorPanel, TchFieldError, TchFormErrorSummary } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import {
  applyServerFieldErrors,
  clearServerFieldErrors,
  clearServerFieldErrorsOnEdit,
  ErrorViewModel,
  toErrorViewModel,
  withResolvedErrorCopies,
} from '@tch/web/errors';
import {
  SellerTerminalApi,
  SellerTerminalSummaryRow,
} from '../../../data-access/seller-terminal-api.service';
import { SellerTerminalDialogResult } from './seller-terminal-dialog-result';

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
    TchFormErrorSummary,
    TranslatePipe,
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
  private readonly dialogRef = inject(
    MatDialogRef<BlockSellerTerminalDialog, SellerTerminalDialogResult>,
  );
  private readonly api = inject(SellerTerminalApi);
  private readonly fb = inject(FormBuilder);
  private readonly translate = inject(TranslateService);
  private readonly destroyRef = inject(DestroyRef);

  readonly saving = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly formSummary = signal<readonly string[]>([]);

  readonly form = this.fb.nonNullable.group({
    reason: ['', [Validators.required, Validators.maxLength(200)]],
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

    this.saving.set(true);
    this.api
      .block(this.data.id.value, this.form.controls.reason.value, { suppressShellFeedback: true })
      .subscribe({
        next: () =>
          this.dialogRef.close({
            reload: true,
            noticeKey: 'admin.sellerTerminals.list.notice.blocked',
          }),
        error: (err: unknown) => {
          this.handleSubmitError(err);
          this.saving.set(false);
        },
      });
  }

  private handleSubmitError(err: unknown): void {
    const problem = mapHttpErrorToProblemDetail(err);
    const fieldErrors = withResolvedErrorCopies(
      webAppErrorsFromProblemDetailFields(problem, 'admin.sellerTerminal.block'),
      key => this.translate.instant(key),
    );
    const remaining = applyServerFieldErrors(this.form, fieldErrors, BLOCK_FIELD_TARGETS);
    if (fieldErrors.length && !remaining.length) {
      this.error.set(null);
      return;
    }

    if (remaining.length) {
      this.formSummary.set(this.messagesForErrors(remaining));
      this.error.set(null);
      return;
    }

    const normalized = webAppErrorFromProblemDetail(problem, 'admin.sellerTerminal.block', 'form');
    const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
    this.error.set(toErrorViewModel(normalized, copy));
  }

  private messagesForErrors(errors: readonly { readonly message?: string }[]): readonly string[] {
    return errors.flatMap(error => (error.message ? [error.message] : []));
  }
}
