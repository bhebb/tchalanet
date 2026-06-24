import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

import { DrawLifecycleAction, GeneratedDrawView } from '../../../data-access/admin-generated-draws.models';

export interface AdminDrawLifecycleDialogData {
  draw: GeneratedDrawView;
  action: DrawLifecycleAction;
}

const ACTION_CONFIG: Record<DrawLifecycleAction, { label: string; reasonRequired: boolean; color: string }> = {
  lock:    { label: 'Verrouiller', reasonRequired: false, color: 'primary' },
  unlock:  { label: 'Déverrouiller', reasonRequired: false, color: 'primary' },
  cancel:  { label: 'Annuler le tirage', reasonRequired: true,  color: 'warn' },
  archive: { label: 'Archiver', reasonRequired: false, color: 'primary' },
};

@Component({
  selector: 'tch-admin-draw-lifecycle-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ config.label }} — {{ data.draw.label }}</h2>
    <mat-dialog-content>
      <p class="adld__subtitle">{{ data.draw.businessDate }} · {{ data.draw.slotLabel }}</p>
      <form [formGroup]="form">
        <mat-form-field appearance="outline" class="adld__field">
          <mat-label>{{ config.reasonRequired ? 'Raison (requise)' : 'Raison (optionnelle)' }}</mat-label>
          <textarea matInput formControlName="reason" rows="3"></textarea>
          @if (form.controls['reason'].invalid && form.controls['reason'].touched) {
            <mat-error>Raison requise.</mat-error>
          }
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="null">Annuler</button>
      <button mat-flat-button
        [color]="config.color"
        [disabled]="form.invalid"
        (click)="confirm()">
        {{ config.label }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .adld__subtitle { margin: 0 0 1rem; opacity: .7; font-size: .875rem; }
    .adld__field { width: 100%; }
  `],
})
export class AdminDrawLifecycleDialog {
  protected readonly data = inject<AdminDrawLifecycleDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<AdminDrawLifecycleDialog>);
  private readonly fb = inject(FormBuilder);

  readonly config = ACTION_CONFIG[this.data.action];

  readonly form = this.fb.group({
    reason: ['', this.config.reasonRequired ? [Validators.required, Validators.minLength(3)] : []],
  });

  confirm(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.dialogRef.close({ reason: this.form.value.reason || undefined });
  }
}
