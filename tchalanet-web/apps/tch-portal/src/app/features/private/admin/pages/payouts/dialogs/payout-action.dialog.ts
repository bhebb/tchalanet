import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

const ACTION_LABELS: Record<string, string> = {
  block: 'Bloquer le paiement',
  cancel: 'Annuler le paiement',
  reverse: 'Inverser le paiement',
};

@Component({
  selector: 'tch-payout-action-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  templateUrl: './payout-action.dialog.html',
  styleUrls: ['./payout-action.dialog.scss'],
})
export class PayoutActionDialog {
  protected readonly data = inject<{ action: string; payoutId: string }>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<PayoutActionDialog>);
  private readonly fb = inject(FormBuilder);

  readonly title = ACTION_LABELS[this.data.action] ?? this.data.action;

  readonly form = this.fb.group({
    reason: ['', Validators.required],
  });

  submit(): void {
    if (this.form.invalid) return;
    const reason = this.form.controls.reason.value;
    if (!reason) return;
    this.dialogRef.close(reason);
  }
}
