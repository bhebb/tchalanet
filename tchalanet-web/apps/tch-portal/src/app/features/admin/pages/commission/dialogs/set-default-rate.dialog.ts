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
  template: `
    <h2 mat-dialog-title>Modifier le taux par défaut</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="set-default-rate-dialog__form">
        <mat-form-field appearance="outline" class="set-default-rate-dialog__field">
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
    .set-default-rate-dialog__form { display: flex; flex-direction: column; gap: 0.75rem; }
    .set-default-rate-dialog__field { width: 100%; min-width: 320px; }
  `],
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
    this.dialogRef.close(this.form.controls.rate.value!);
  }
}
