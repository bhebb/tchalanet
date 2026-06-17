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
  template: `
    <h2 mat-dialog-title>{{ title }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="payout-action-dialog__form">
        <mat-form-field appearance="outline" class="payout-action-dialog__field">
          <mat-label>Raison</mat-label>
          <textarea matInput formControlName="reason" rows="3" placeholder="Motif de l'action..."></textarea>
          @if (form.controls.reason.invalid && form.controls.reason.touched) {
            <mat-error>La raison est requise.</mat-error>
          }
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Annuler</button>
      <button mat-flat-button color="warn" [disabled]="form.invalid" (click)="submit()">
        Confirmer
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .payout-action-dialog__form { display: flex; flex-direction: column; gap: 0.75rem; }
    .payout-action-dialog__field { width: 100%; min-width: 360px; }
  `],
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
    this.dialogRef.close(this.form.controls.reason.value!);
  }
}
