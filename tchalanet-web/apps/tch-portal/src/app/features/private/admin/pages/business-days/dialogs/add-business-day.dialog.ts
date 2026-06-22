import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { UpsertBusinessDayRequest } from '../../../business-days-api.service';

@Component({
  selector: 'tch-add-business-day-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
  ],
  template: `
    <h2 mat-dialog-title>Ajouter une exception</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="add-business-day-dialog__form">
        <mat-form-field appearance="outline" class="add-business-day-dialog__field">
          <mat-label>Date (YYYY-MM-DD)</mat-label>
          <input matInput formControlName="date" placeholder="2025-01-01" />
          @if (form.controls.date.invalid && form.controls.date.touched) {
            <mat-error>Date requise (format YYYY-MM-DD).</mat-error>
          }
        </mat-form-field>

        <mat-form-field appearance="outline" class="add-business-day-dialog__field">
          <mat-label>Statut</mat-label>
          <mat-select formControlName="status">
            <mat-option value="OPEN">OPEN (jour ouvrable)</mat-option>
            <mat-option value="CLOSED">CLOSED (jour fermé)</mat-option>
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="outline" class="add-business-day-dialog__field">
          <mat-label>Raison (optionnel)</mat-label>
          <input matInput formControlName="reason" />
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Annuler</button>
      <button mat-flat-button color="primary" [disabled]="form.invalid" (click)="submit()">Ajouter</button>
    </mat-dialog-actions>
  `,
  styles: [`
    .add-business-day-dialog__form { display: flex; flex-direction: column; gap: 0.75rem; width: 100%; }
    .add-business-day-dialog__field { width: 100%; }
  `],
})
export class AddBusinessDayDialog {
  private readonly dialogRef = inject(MatDialogRef<AddBusinessDayDialog>);
  private readonly fb = inject(FormBuilder);

  readonly form = this.fb.group({
    date: ['', [Validators.required, Validators.pattern(/^\d{4}-\d{2}-\d{2}$/)]],
    status: ['CLOSED' as 'OPEN' | 'CLOSED', Validators.required],
    reason: [''],
  });

  submit(): void {
    if (this.form.invalid) return;
    const v = this.form.value;
    const req: UpsertBusinessDayRequest = {
      date: v.date!,
      status: v.status!,
      reason: v.reason || undefined,
    };
    this.dialogRef.close(req);
  }
}
