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
  template: `
    <h2 mat-dialog-title>Taux — {{ data.row.displayName }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="set-seller-rate-dialog__form">
        <mat-form-field appearance="outline" class="set-seller-rate-dialog__field">
          <mat-label>Taux de commission (%)</mat-label>
          <input matInput type="number" formControlName="rate" min="0" max="100" step="0.01" />
          @if (form.controls.rate.invalid && form.controls.rate.touched) {
            <mat-error>Taux entre 0 et 100.</mat-error>
          }
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Annuler</button>
      <button mat-flat-button color="primary" [disabled]="form.invalid" (click)="submit()">
        Enregistrer
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .set-seller-rate-dialog__form { display: flex; flex-direction: column; gap: 0.75rem; }
    .set-seller-rate-dialog__field { width: 100%; min-width: 320px; }
  `],
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
    this.dialogRef.close(this.form.controls.rate.value!);
  }
}
