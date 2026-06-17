import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';

import { SellerTerminalSummaryRow } from '../../../seller-terminal-api.service';

@Component({
  selector: 'tch-confirm-disable-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatDialogModule],
  template: `
    <h2 mat-dialog-title>Désactiver le vendeur</h2>
    <mat-dialog-content>
      <p>Désactiver <strong>{{ data.displayName }}</strong> ({{ data.terminalCode }}) ?</p>
      <p class="confirm-disable-dialog__warning">Cette action est difficile à annuler.</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="false">Annuler</button>
      <button mat-flat-button color="warn" [mat-dialog-close]="true">Désactiver</button>
    </mat-dialog-actions>
  `,
  styles: [`
    .confirm-disable-dialog__warning { color: var(--tch-color-error); font-size: 0.875rem; margin: 0; }
  `],
})
export class ConfirmDisableDialog {
  protected readonly data = inject<SellerTerminalSummaryRow>(MAT_DIALOG_DATA);
}
