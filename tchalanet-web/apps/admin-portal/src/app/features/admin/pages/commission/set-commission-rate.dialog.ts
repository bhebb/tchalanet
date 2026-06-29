import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';

export interface SetRateDialogData {
  readonly title: string;
  readonly currentRate: number;
  readonly label: string;
}

export interface SetRateDialogResult {
  readonly rate: number;
}

@Component({
  selector: 'tch-set-commission-rate-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
  ],
  templateUrl: './set-commission-rate.dialog.html',
  styleUrls: ['./set-commission-rate.dialog.scss'],
})
export class SetCommissionRateDialog {
  protected readonly data = inject<SetRateDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<SetCommissionRateDialog, SetRateDialogResult>);

  readonly rateControl = inject(FormBuilder).nonNullable.control(
    this.data.currentRate,
    [Validators.required, Validators.min(0), Validators.max(100)],
  );

  confirm(): void {
    if (this.rateControl.invalid) return;
    this.dialogRef.close({ rate: this.rateControl.value });
  }
}
