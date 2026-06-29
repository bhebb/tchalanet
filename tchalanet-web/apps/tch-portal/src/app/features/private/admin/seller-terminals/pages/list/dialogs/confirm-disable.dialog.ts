import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';

import { SellerTerminalSummaryRow } from '../../../../seller-terminal-api.service';

@Component({
  selector: 'tch-confirm-disable-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatDialogModule],
  templateUrl: './confirm-disable.dialog.html',
  styleUrls: ['./confirm-disable.dialog.scss'],
})
export class ConfirmDisableDialog {
  protected readonly data = inject<SellerTerminalSummaryRow>(MAT_DIALOG_DATA);
}
