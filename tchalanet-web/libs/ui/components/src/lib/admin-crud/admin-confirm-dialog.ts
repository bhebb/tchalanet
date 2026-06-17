import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';

export interface AdminConfirmDialogData {
  readonly title: string;
  readonly message: string;
  readonly confirmLabel: string;
  readonly cancelLabel?: string;
  readonly destructive?: boolean;
}

@Component({
  selector: 'tch-admin-confirm-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>{{ data.title }}</h2>
    <mat-dialog-content>
      <p class="confirm-dialog__message">{{ data.message }}</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>
        {{ data.cancelLabel ?? 'Annuler' }}
      </button>
      <button
        mat-flat-button
        [class.confirm-dialog__btn--destructive]="data.destructive"
        (click)="confirm()"
      >
        {{ data.confirmLabel }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      .confirm-dialog__message {
        margin: 0;
        color: var(--tch-color-on-surface-variant);
        font-size: var(--tch-font-size-body-md, 1rem);
        line-height: 1.5;
      }

      .confirm-dialog__btn--destructive {
        --mdc-filled-button-container-color: var(--tch-color-error);
        --mdc-filled-button-label-text-color: var(--tch-color-on-error);
      }
    `,
  ],
})
export class AdminConfirmDialog {
  protected readonly data = inject<AdminConfirmDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<AdminConfirmDialog, boolean>);

  confirm(): void {
    this.dialogRef.close(true);
  }
}
