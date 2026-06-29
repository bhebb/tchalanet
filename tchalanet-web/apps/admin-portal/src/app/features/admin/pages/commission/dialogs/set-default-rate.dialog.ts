import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

@Component({
  selector: 'tch-set-default-rate-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  templateUrl: './set-default-rate.dialog.html',
  styleUrls: ['./set-default-rate.dialog.scss'],
})
export class SetDefaultRateDialog {
  protected readonly data = inject<{ current: number }>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<SetDefaultRateDialog>);
  private readonly fb = inject(FormBuilder);

  readonly form = this.fb.group({
    rate: [this.data.current, [Validators.required, Validators.min(0), Validators.max(100)]],
  });

  submit(): void {
    if (this.form.invalid) return;
    const rate = this.form.controls.rate.value;
    if (rate == null) return;
    this.dialogRef.close(rate);
  }
}
