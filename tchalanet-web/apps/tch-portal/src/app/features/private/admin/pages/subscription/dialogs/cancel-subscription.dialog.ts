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
  template: `
    <h2 mat-dialog-title>Annuler l'abonnement</h2>
    <mat-dialog-content>
      <p class="cancel-subscription-dialog__warning">
        Cette action annule l'abonnement du tenant. Elle est difficile à annuler.
      </p>
      <form [formGroup]="form" class="cancel-subscription-dialog__form">
        <mat-form-field appearance="outline" class="cancel-subscription-dialog__field">
          <mat-label>Raison (optionnel)</mat-label>
          <textarea matInput formControlName="reason" rows="2" placeholder="Motif d'annulation..."></textarea>
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Annuler</button>
      <button mat-flat-button color="warn" (click)="submit()">
        Confirmer l'annulation
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .cancel-subscription-dialog__warning {
      color: var(--tch-color-error);
      font-size: 0.875rem;
      margin: 0 0 1rem;
    }
    .cancel-subscription-dialog__form { display: flex; flex-direction: column; gap: 0.75rem; }
    .cancel-subscription-dialog__field { width: 100%; min-width: 360px; }
  `],
})
export class CancelSubscriptionDialog {
  private readonly dialogRef = inject(MatDialogRef<CancelSubscriptionDialog>);
  private readonly fb = inject(FormBuilder);

  readonly form = this.fb.group({ reason: [''] });

  submit(): void {
    this.dialogRef.close(this.form.controls.reason.value ?? '');
  }
}
