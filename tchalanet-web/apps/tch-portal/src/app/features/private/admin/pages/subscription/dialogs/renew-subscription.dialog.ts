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
  template: `
    <h2 mat-dialog-title>Renouveler l'abonnement</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="renew-subscription-dialog__form">
        <mat-form-field appearance="outline" class="renew-subscription-dialog__field">
          <mat-label>Nouvelle date de fin (YYYY-MM-DD)</mat-label>
          <input matInput formControlName="endsAt" placeholder="2027-01-01" />
          @if (form.controls.endsAt.invalid && form.controls.endsAt.touched) {
            <mat-error>Date requise (format YYYY-MM-DD).</mat-error>
          }
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Annuler</button>
      <button mat-flat-button color="primary" [disabled]="form.invalid" (click)="submit()">
        Renouveler
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .renew-subscription-dialog__form { display: flex; flex-direction: column; gap: 0.75rem; }
    .renew-subscription-dialog__field { width: 100%; min-width: 340px; }
  `],
})
export class RenewSubscriptionDialog {
  private readonly dialogRef = inject(MatDialogRef<RenewSubscriptionDialog>);
  private readonly fb = inject(FormBuilder);

  readonly form = this.fb.group({
    endsAt: ['', [Validators.required, Validators.pattern(/^\d{4}-\d{2}-\d{2}$/)]],
  });

  submit(): void {
    if (this.form.invalid) return;
    this.dialogRef.close(this.form.controls.endsAt.value!);
  }
}
