import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { Clipboard } from '@angular/cdk/clipboard';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import {
  mapHttpErrorToProblemDetail,
  webAppErrorFromProblemDetail,
  webAppErrorsFromProblemDetailFields,
} from '@tch/api';

import { TchErrorPanel, TchFieldError, TchFormErrorSummary } from '@tch/ui/components';
import {
  applyServerFieldErrors,
  clearServerFieldErrors,
  clearServerFieldErrorsOnEdit,
  ErrorViewModel,
  resolveErrorFeedbackCopy,
  toErrorViewModel,
  withResolvedErrorCopies,
} from '@tch/web/errors';
import {
  PinResetReason,
  ResetSellerTerminalPinResponse,
  SellerTerminalApi,
  SellerTerminalSummaryRow,
} from '../../../data-access/seller-terminal-api.service';

type DialogState = 'confirming' | 'submitting' | 'success' | 'error';

const PIN_RESET_FIELD_TARGETS = {
  reason: 'reason',
  'admin.sellerTerminal.resetPin.reason': 'reason',
} as const;

@Component({
  selector: 'tch-reset-pin-dialog',
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
    MatIconModule,
    MatSelectModule,
  ],
  templateUrl: './reset-pin.dialog.html',
  styleUrls: ['./reset-pin.dialog.scss'],
})
export class ResetPinDialog {
  protected readonly data = inject<SellerTerminalSummaryRow>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<ResetPinDialog>);
  private readonly api = inject(SellerTerminalApi);
  private readonly clipboard = inject(Clipboard);
  private readonly fb = inject(FormBuilder);
  private readonly translate = inject(TranslateService);
  private readonly destroyRef = inject(DestroyRef);

  readonly state = signal<DialogState>('confirming');
  readonly result = signal<ResetSellerTerminalPinResponse | null>(null);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly formSummary = signal<readonly string[]>([]);
  readonly pinCopied = signal(false);

  readonly form = this.fb.group({
    reason: [null as PinResetReason | null, Validators.required],
  });

  constructor() {
    const cleanup = clearServerFieldErrorsOnEdit(this.form);
    this.destroyRef.onDestroy(() => cleanup.unsubscribe());
  }

  submit(): void {
    if (this.form.invalid || this.state() === 'submitting') return;
    this.state.set('submitting');
    clearServerFieldErrors(this.form);
    this.error.set(null);
    this.formSummary.set([]);

    const reason = this.form.controls.reason.value;
    if (!reason) {
      this.state.set('confirming');
      this.form.controls.reason.markAsTouched();
      return;
    }

    this.api.resetPin(this.data.id.value, { reason }, { suppressShellFeedback: true }).subscribe({
      next: res => {
        this.result.set(res);
        this.state.set('success');
      },
      error: (err: unknown) => {
        this.handleSubmitError(err);
        this.state.set('confirming');
      },
    });
  }

  copyPin(): void {
    const pin = this.result()?.temporaryPin;
    if (!pin) return;
    this.clipboard.copy(pin);
    this.pinCopied.set(true);
    setTimeout(() => this.pinCopied.set(false), 2000);
  }

  close(): void {
    this.result.set(null);
    this.dialogRef.close({ reload: true });
  }

  private handleSubmitError(err: unknown): void {
    const problem = mapHttpErrorToProblemDetail(err);
    const fieldErrors = withResolvedErrorCopies(
      webAppErrorsFromProblemDetailFields(problem, 'admin.sellerTerminal.resetPin'),
      key => this.translate.instant(key),
    );
    const remaining = applyServerFieldErrors(this.form, fieldErrors, PIN_RESET_FIELD_TARGETS);
    if (fieldErrors.length && remaining.length) {
      this.formSummary.set(remaining.flatMap(item => (item.message ? [item.message] : [])));
      return;
    }
    if (fieldErrors.length) return;

    const normalized = webAppErrorFromProblemDetail(
      problem,
      'admin.sellerTerminal.resetPin',
      'form',
    );
    const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
    this.error.set(toErrorViewModel(normalized, copy));
  }
}
