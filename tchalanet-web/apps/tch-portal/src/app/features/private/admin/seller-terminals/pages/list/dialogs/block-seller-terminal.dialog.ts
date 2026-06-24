import {
  ChangeDetectionStrategy,
  Component,
  inject,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

import { TchErrorPanel } from '@tch/ui/components';
import { SellerTerminalApi, SellerTerminalSummaryRow } from '../../../../seller-terminal-api.service';

@Component({
  selector: 'tch-block-seller-terminal-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    TchErrorPanel,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  template: `
    <h2 mat-dialog-title>Bloquer le vendeur</h2>
    <mat-dialog-content>
      <p class="block-dialog__info">
        Bloquer <strong>{{ data.displayName }}</strong> ({{ data.terminalCode }})
        empêche toute vente jusqu'au déblocage.
      </p>
      <form [formGroup]="form">
        <mat-form-field appearance="outline" style="width:100%">
          <mat-label>Raison du blocage</mat-label>
          <textarea
            matInput
            formControlName="reason"
            rows="3"
            maxlength="200"
            placeholder="Ex. : terminal non retourné, suspicion de fraude…"
          ></textarea>
          @if (form.controls.reason.invalid && form.controls.reason.touched) {
            <mat-error>Raison requise.</mat-error>
          }
        </mat-form-field>
        @if (error()) {
          <tch-error-panel [title]="error()!" />
        }
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close [disabled]="saving()">Annuler</button>
      <button
        mat-flat-button
        color="warn"
        [disabled]="form.invalid || saving()"
        (click)="submit()"
      >
        @if (saving()) {
          <span class="material-symbols-outlined block-dialog__spin">progress_activity</span>
        }
        Bloquer
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .block-dialog__info {
      color: var(--mat-sys-on-surface-variant);
      font-size: 0.875rem;
      line-height: 1.5;
      margin-bottom: 1rem;
    }
    .block-dialog__spin {
      animation: block-spin 0.8s linear infinite;
      display: inline-block;
      vertical-align: middle;
    }
    @keyframes block-spin { to { transform: rotate(360deg); } }
  `],
})
export class BlockSellerTerminalDialog {
  protected readonly data = inject<SellerTerminalSummaryRow>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<BlockSellerTerminalDialog>);
  private readonly api = inject(SellerTerminalApi);
  private readonly fb = inject(FormBuilder);

  readonly saving = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    reason: ['', [Validators.required, Validators.maxLength(200)]],
  });

  submit(): void {
    if (this.form.invalid || this.saving()) return;
    this.saving.set(true);
    this.error.set(null);
    this.api.block(this.data.id.value, this.form.controls.reason.value).subscribe({
      next: () => this.dialogRef.close({ reload: true }),
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string } })?.error;
        this.error.set(pd?.title ?? 'Erreur lors du blocage.');
        this.saving.set(false);
      },
    });
  }
}
