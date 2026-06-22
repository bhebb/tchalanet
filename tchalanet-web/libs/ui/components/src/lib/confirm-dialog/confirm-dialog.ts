import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';

export interface TchConfirmDialogData {
  readonly title: string;
  readonly message: string;
  readonly confirmLabel: string;
  readonly cancelLabel?: string;
  /** Render the confirm button with the error colour (irreversible actions). */
  readonly destructive?: boolean;
  /** Sensitive/audited mode: navy security header, AUDITÉ badge, confirmation checkbox. */
  readonly sensitive?: boolean;
  /** Show a required reason textarea. */
  readonly requireReason?: boolean;
  /** Minimum reason length (default 10). */
  readonly reasonMinLength?: number;
  /** Label for the reason field. Falls back to {@link auditLabel}, then 'Motif'. */
  readonly reasonLabel?: string;
  /** Eyebrow text in the security header. */
  readonly auditLabel?: string;
  /** Label for the acknowledgement checkbox (sensitive mode). */
  readonly confirmCheckboxLabel?: string;
  /** Optional leading icon (Material Symbols name) shown next to the message. */
  readonly icon?: string;
}

export interface TchConfirmDialogResult {
  readonly confirmed: boolean;
  readonly reason?: string;
}

/**
 * Generic confirmation dialog. Supports a sensitive/audited mode (reason + acknowledgement)
 * for super-admin support actions. Opened via MatDialog; returns `undefined` on cancel.
 */
@Component({
  selector: 'tch-confirm-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, MatDialogModule, MatButtonModule, MatCheckboxModule],
  templateUrl: './confirm-dialog.html',
  styleUrl: './confirm-dialog.scss',
})
export class TchConfirmDialog {
  protected readonly data = inject<TchConfirmDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<TchConfirmDialog, TchConfirmDialogResult>);

  protected readonly reasonId = `tch-confirm-reason-${Math.random().toString(36).slice(2, 9)}`;
  protected readonly reason = signal('');
  protected readonly acknowledged = signal(false);

  protected readonly reasonMin = computed(() => this.data.reasonMinLength ?? 10);

  protected readonly reasonHint = computed(() => {
    const len = this.reason().trim().length;
    const min = this.reasonMin();
    return len >= min ? `${len}` : `Min. ${min} caractères`;
  });

  protected readonly canConfirm = computed(() => {
    const reasonOk = !this.data.requireReason || this.reason().trim().length >= this.reasonMin();
    const ackOk = !this.data.sensitive || this.acknowledged();
    return reasonOk && ackOk;
  });

  cancel(): void {
    this.dialogRef.close(undefined);
  }

  confirm(): void {
    if (!this.canConfirm()) return;
    this.dialogRef.close({
      confirmed: true,
      reason: this.data.requireReason ? this.reason().trim() : undefined,
    });
  }
}
