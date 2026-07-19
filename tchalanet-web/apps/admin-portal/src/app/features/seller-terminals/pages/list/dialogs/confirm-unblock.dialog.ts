import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { mapHttpErrorToProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { TchErrorPanel } from '@tch/ui/components';
import { ErrorViewModel, resolveErrorFeedbackCopy, toErrorViewModel } from '@tch/web/errors';

import {
  SellerTerminalApi,
  SellerTerminalSummaryRow,
} from '../../../data-access/seller-terminal-api.service';

@Component({
  selector: 'tch-confirm-unblock-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatDialogModule, TranslatePipe, TchErrorPanel],
  templateUrl: './confirm-unblock.dialog.html',
  styleUrls: ['./confirm-disable.dialog.scss'],
})
export class ConfirmUnblockDialog {
  protected readonly data = inject<SellerTerminalSummaryRow>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<ConfirmUnblockDialog, boolean>);
  private readonly api = inject(SellerTerminalApi);
  private readonly translate = inject(TranslateService);

  readonly saving = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);

  submit(): void {
    if (this.saving()) return;
    this.saving.set(true);
    this.error.set(null);
    this.api.unblock(this.data.id.value, { suppressShellFeedback: true }).subscribe({
      next: () => this.dialogRef.close(true),
      error: (err: unknown) => {
        const normalized = webAppErrorFromProblemDetail(
          mapHttpErrorToProblemDetail(err),
          'admin.sellerTerminal.unblock',
          'form',
        );
        const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
        this.error.set(toErrorViewModel(normalized, copy));
        this.saving.set(false);
      },
    });
  }
}
