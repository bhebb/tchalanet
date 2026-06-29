import {
  ChangeDetectionStrategy,
  Component,
  inject,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { Clipboard } from '@angular/cdk/clipboard';
import { TranslateService } from '@ngx-translate/core';
import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';

import { TchErrorPanel } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import {
  PinResetReason,
  ResetSellerTerminalPinResponse,
  SellerTerminalApi,
  SellerTerminalSummaryRow,
} from '../../../../seller-terminal-api.service';

type DialogState = 'confirming' | 'submitting' | 'success' | 'error';

@Component({
  selector: 'tch-reset-pin-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    TchErrorPanel,
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

  readonly state = signal<DialogState>('confirming');
  readonly result = signal<ResetSellerTerminalPinResponse | null>(null);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly pinCopied = signal(false);

  readonly form = this.fb.group({
    reason: [null as PinResetReason | null, Validators.required],
  });

  submit(): void {
    if (this.form.invalid || this.state() === 'submitting') return;
    this.state.set('submitting');
    this.error.set(null);

    const reason = this.form.controls.reason.value;
    if (!reason) {
      this.state.set('confirming');
      this.form.controls.reason.markAsTouched();
      return;
    }

    this.api
      .resetPin(this.data.id.value, { reason }, { suppressShellFeedback: true })
      .subscribe({
        next: res => {
          this.result.set(res);
          this.state.set('success');
        },
        error: (err: unknown) => {
          this.error.set(this.errorViewModel(err));
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

  private errorViewModel(err: unknown): ErrorViewModel {
    const problem = (err as { error?: ProblemDetail })?.error;
    if (problem) {
      const normalized = webAppErrorFromProblemDetail(problem, 'admin.sellerTerminal.resetPin', 'page');
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
