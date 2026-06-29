import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

@Component({
  selector: 'tch-renew-subscription-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  templateUrl: './renew-subscription.dialog.html',
  styleUrls: ['./renew-subscription.dialog.scss'],
})
export class RenewSubscriptionDialog {
  private readonly dialogRef = inject(MatDialogRef<RenewSubscriptionDialog>);
  private readonly fb = inject(FormBuilder);

  readonly form = this.fb.group({
    endsAt: ['', [Validators.required, Validators.pattern(/^\d{4}-\d{2}-\d{2}$/)]],
  });

  submit(): void {
    if (this.form.invalid) return;
    const endsAt = this.form.controls.endsAt.value;
    if (!endsAt) return;
    this.dialogRef.close(endsAt);
  }
}
