import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

import { DrawView } from '../../data-access/platform-ops-api.service';

export type DrawAction = 'cancel' | 'lock' | 'unlock' | 'settle' | 'archive' | 'reschedule';

export interface ActionDialogData {
  draw: DrawView;
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
  templateUrl: './draw-lifecycle-action.dialog.html',
  styleUrls: ['./draw-lifecycle-action.dialog.scss'],
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
