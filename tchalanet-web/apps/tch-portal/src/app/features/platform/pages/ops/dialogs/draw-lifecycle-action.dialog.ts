import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

import { DrawSummaryResponse } from '../../../platform-ops-api.service';

export type DrawAction = 'cancel' | 'lock' | 'unlock' | 'settle' | 'archive' | 'reschedule';

export interface ActionDialogData {
  draw: DrawSummaryResponse;
  action: DrawAction;
}

export interface ActionDialogResult {
  reason?: string;
  newScheduledAt?: string;
}

export const ACTION_LABELS: Record<DrawAction, string> = {
  cancel: 'Annuler',
  lock: 'Verrouiller',
  unlock: 'Déverrouiller',
  settle: 'Régler',
  archive: 'Archiver',
  reschedule: 'Reprogrammer',
};

@Component({
  selector: 'tch-draw-lifecycle-action-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ actionLabel }} — {{ data.draw.channelName }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="lifecycle-action-dialog__form">
        @if (data.action === 'reschedule') {
          <mat-form-field appearance="outline" class="lifecycle-action-dialog__field">
            <mat-label>Nouvelle date/heure</mat-label>
            <input matInput formControlName="newScheduledAt" type="datetime-local" />
            @if (form.controls['newScheduledAt'].invalid && form.controls['newScheduledAt'].touched) {
              <mat-error>Date/heure requise.</mat-error>
            }
          </mat-form-field>
        }
        @if (needsReason) {
          <mat-form-field appearance="outline" class="lifecycle-action-dialog__field">
            <mat-label>{{ data.action === 'cancel' ? 'Raison (requise)' : 'Raison (optionnelle)' }}</mat-label>
            <textarea matInput formControlName="reason" rows="3"></textarea>
            @if (form.controls['reason'].invalid && form.controls['reason'].touched) {
              <mat-error>Raison requise.</mat-error>
            }
          </mat-form-field>
        }
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="null">Annuler</button>
      <button mat-flat-button color="primary" [disabled]="form.invalid" (click)="confirm()">
        {{ actionLabel }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .lifecycle-action-dialog__form {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
      width: 100%;
    }
    .lifecycle-action-dialog__field {
      width: 100%;
    }
  `],
})
export class DrawLifecycleActionDialog {
  protected readonly data = inject<ActionDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<DrawLifecycleActionDialog>);
  private readonly fb = inject(FormBuilder);

  get actionLabel(): string {
    return ACTION_LABELS[this.data.action];
  }

  get needsReason(): boolean {
    return ['cancel', 'lock', 'unlock'].includes(this.data.action);
  }

  readonly form = this.fb.group({
    reason: [
      '',
      this.data.action === 'cancel' ? [Validators.required, Validators.minLength(3)] : [],
    ],
    newScheduledAt: [
      '',
      this.data.action === 'reschedule' ? [Validators.required] : [],
    ],
  });

  confirm(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.value;
    const result: ActionDialogResult = {};
    if (v.reason) result.reason = v.reason;
    if (v.newScheduledAt) result.newScheduledAt = v.newScheduledAt;
    this.dialogRef.close(result);
  }
}
