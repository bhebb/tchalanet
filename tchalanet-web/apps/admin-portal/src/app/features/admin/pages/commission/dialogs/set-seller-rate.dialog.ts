import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

import { SellerTerminalCommissionRow } from '../../../admin-commission-api.service';

@Component({
  selector: 'tch-set-seller-rate-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  templateUrl: './set-seller-rate.dialog.html',
  styleUrls: ['./set-seller-rate.dialog.scss'],
})
export class SetSellerRateDialog {
  protected readonly data = inject<{ row: SellerTerminalCommissionRow }>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<SetSellerRateDialog>);
  private readonly fb = inject(FormBuilder);

  readonly form = this.fb.group({
    rate: [this.data.row.commissionRate, [Validators.required, Validators.min(0), Validators.max(100)]],
  });

  submit(): void {
    if (this.form.invalid) return;
    const rate = this.form.controls.rate.value;
    if (rate == null) return;
    this.dialogRef.close(rate);
  }
}
