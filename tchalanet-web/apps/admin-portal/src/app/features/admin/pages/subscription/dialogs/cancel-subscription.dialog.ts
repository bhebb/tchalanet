import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

@Component({
  selector: 'tch-cancel-subscription-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  templateUrl: './cancel-subscription.dialog.html',
  styleUrls: ['./cancel-subscription.dialog.scss'],
})
export class CancelSubscriptionDialog {
  private readonly dialogRef = inject(MatDialogRef<CancelSubscriptionDialog>);
  private readonly fb = inject(FormBuilder);

  readonly form = this.fb.group({ reason: [''] });

  submit(): void {
    this.dialogRef.close(this.form.controls.reason.value ?? '');
  }
}
