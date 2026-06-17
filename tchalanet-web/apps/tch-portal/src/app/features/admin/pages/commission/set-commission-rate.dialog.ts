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
  template: `
    <h2 mat-dialog-title>{{ data.title }}</h2>
    <mat-dialog-content>
      <mat-form-field appearance="outline" style="width:100%;margin-top:.75rem">
        <mat-label>{{ data.label }}</mat-label>
        <input
          matInput
          type="number"
          min="0"
          max="100"
          step="0.01"
          [formControl]="rateControl"
        />
        <span matSuffix>%</span>
        @if (rateControl.hasError('required')) {
          <mat-error>Valeur obligatoire.</mat-error>
        }
        @if (rateControl.hasError('min') || rateControl.hasError('max')) {
          <mat-error>Le taux doit être entre 0 et 100.</mat-error>
        }
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Annuler</button>
      <button mat-flat-button (click)="confirm()" [disabled]="rateControl.invalid">
        Appliquer
      </button>
    </mat-dialog-actions>
  `,
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
