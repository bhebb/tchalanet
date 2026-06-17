import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';

import { TchErrorPanel } from '@tch/ui/components';
import { SellerTerminalApi, SellerTerminalSummaryRow } from '../../../seller-terminal-api.service';

@Component({
  selector: 'tch-reset-pin-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    TchErrorPanel,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
  ],
  template: `
    <h2 mat-dialog-title>Réinitialiser le PIN — {{ data.displayName }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="reset-pin-dialog__form">
        <mat-form-field appearance="outline" class="reset-pin-dialog__field">
          <mat-label>Nouveau PIN</mat-label>
          <input matInput formControlName="pin" type="password" placeholder="4 à 8 chiffres" />
          @if (form.controls.pin.invalid && form.controls.pin.touched) {
            <mat-error>4 à 8 chiffres uniquement.</mat-error>
          }
        </mat-form-field>
        @if (error()) {
          <tch-error-panel [title]="error()!" />
        }
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close [disabled]="saving()">Annuler</button>
      <button mat-flat-button color="primary" [disabled]="form.invalid || saving()" (click)="submit()">
        @if (saving()) {
          <span class="material-symbols-outlined spin" aria-hidden="true">progress_activity</span>
        }
        Réinitialiser
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .reset-pin-dialog__form { display: flex; flex-direction: column; gap: 0.75rem; width: 100%; }
    .reset-pin-dialog__field { width: 100%; }
    .spin { animation: spin 0.8s linear infinite; display: inline-block; vertical-align: middle; }
    @keyframes spin { to { transform: rotate(360deg); } }
  `],
})
export class ResetPinDialog {
  protected readonly data = inject<SellerTerminalSummaryRow>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<ResetPinDialog>);
  private readonly api = inject(SellerTerminalApi);
  private readonly snackBar = inject(MatSnackBar);
  private readonly fb = inject(FormBuilder);

  readonly saving = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.group({
    pin: ['', [Validators.required, Validators.pattern(/^\d{4,8}$/)]],
  });

  submit(): void {
    if (this.form.invalid || this.saving()) return;
    this.saving.set(true);
    this.error.set(null);
    this.api.resetAccess(this.data.id.value, this.form.controls.pin.value!).subscribe({
      next: () => {
        this.saving.set(false);
        this.snackBar.open('PIN réinitialisé.', 'OK', { duration: 3000 });
        this.dialogRef.close(true);
      },
      error: (err: unknown) => {
        this.saving.set(false);
        const pd = (err as { error?: { title?: string } })?.error;
        this.error.set(pd?.title ?? 'Erreur lors de la réinitialisation.');
      },
    });
  }
}
