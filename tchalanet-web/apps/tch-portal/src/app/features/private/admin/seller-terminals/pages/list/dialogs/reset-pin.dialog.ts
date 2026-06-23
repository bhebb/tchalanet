import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Clipboard } from '@angular/cdk/clipboard';

import { TchErrorPanel } from '@tch/ui/components';
import {
  PinResetReason,
  ResetSellerTerminalPinResponse,
  SellerTerminalApi,
  SellerTerminalSummaryRow,
} from '../../../../seller-terminal-api.service';

type DialogState = 'confirming' | 'submitting' | 'success' | 'error';

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
    MatSelectModule,
  ],
  template: `
    <!-- ── Confirmation ───────────────────────────────────────────── -->
    @if (state() === 'confirming' || state() === 'submitting') {
      <h2 mat-dialog-title>Réinitialiser le PIN</h2>
      <mat-dialog-content>
        <p class="reset-pin-dialog__info">
          Un nouveau PIN temporaire sera généré pour
          <strong>{{ data.terminalCode }}</strong> — {{ data.displayName }}.<br />
          L'ancien PIN ne fonctionnera plus.<br />
          Le vendeur devra changer son PIN à sa prochaine connexion.
        </p>
        <form [formGroup]="form" class="reset-pin-dialog__form">
          <mat-form-field appearance="outline" class="reset-pin-dialog__field">
            <mat-label>Raison</mat-label>
            <mat-select formControlName="reason">
              <mat-option value="PIN_LOST">PIN perdu</mat-option>
              <mat-option value="SELLER_CHANGED">Changement de vendeur</mat-option>
              <mat-option value="SUSPECTED_COMPROMISE">Suspicion de compromission</mat-option>
              <mat-option value="ADMIN_CORRECTION">Correction administrative</mat-option>
              <mat-option value="OTHER">Autre</mat-option>
            </mat-select>
            @if (form.controls.reason.invalid && form.controls.reason.touched) {
              <mat-error>Veuillez sélectionner une raison.</mat-error>
            }
          </mat-form-field>
          @if (errorMessage()) {
            <tch-error-panel [title]="errorMessage()!" />
          }
        </form>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button mat-dialog-close [disabled]="state() === 'submitting'">Annuler</button>
        <button
          mat-flat-button
          color="warn"
          [disabled]="form.invalid || state() === 'submitting'"
          (click)="submit()"
        >
          @if (state() === 'submitting') {
            <span class="material-symbols-outlined spin" aria-hidden="true">progress_activity</span>
          }
          Réinitialiser le PIN
        </button>
      </mat-dialog-actions>
    }

    <!-- ── Success ────────────────────────────────────────────────── -->
    @if (state() === 'success') {
      <h2 mat-dialog-title>PIN temporaire généré</h2>
      <mat-dialog-content class="reset-pin-dialog__success">
        <p>
          <strong>Terminal :</strong> {{ result()!.terminalCode }}
        </p>
        <div class="reset-pin-dialog__pin-box">
          <span class="reset-pin-dialog__pin">{{ result()!.temporaryPin }}</span>
          <button mat-icon-button (click)="copyPin()" [title]="pinCopied() ? 'Copié !' : 'Copier le PIN'">
            <span class="material-symbols-outlined">{{ pinCopied() ? 'check' : 'content_copy' }}</span>
          </button>
        </div>
        <p class="reset-pin-dialog__warning">
          <span class="material-symbols-outlined reset-pin-dialog__warning-icon">warning</span>
          Copiez ce PIN maintenant. Il ne sera plus affiché après fermeture.<br />
          Le vendeur devra le changer à sa prochaine connexion.
        </p>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-flat-button color="primary" (click)="close()">Fermer</button>
      </mat-dialog-actions>
    }
  `,
  styles: [`
    .reset-pin-dialog__info {
      color: var(--mat-sys-on-surface-variant);
      font-size: 0.875rem;
      line-height: 1.5;
      margin-bottom: 1rem;
    }
    .reset-pin-dialog__form { display: flex; flex-direction: column; gap: 0.5rem; }
    .reset-pin-dialog__field { width: 100%; }
    .reset-pin-dialog__success { display: flex; flex-direction: column; gap: 0.75rem; }
    .reset-pin-dialog__pin-box {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      background: var(--mat-sys-surface-container-high);
      border-radius: 8px;
      padding: 0.75rem 1rem;
    }
    .reset-pin-dialog__pin {
      font-size: 2rem;
      font-weight: 700;
      letter-spacing: 0.4rem;
      font-variant-numeric: tabular-nums;
      flex: 1;
    }
    .reset-pin-dialog__warning {
      display: flex;
      align-items: flex-start;
      gap: 0.5rem;
      font-size: 0.8125rem;
      color: var(--tch-color-error, #ba1a1a);
      line-height: 1.4;
    }
    .reset-pin-dialog__warning-icon {
      font-size: 1.1rem;
      flex-shrink: 0;
      margin-top: 0.1rem;
    }
    .spin { animation: spin 0.8s linear infinite; display: inline-block; vertical-align: middle; }
    @keyframes spin { to { transform: rotate(360deg); } }
  `],
})
export class ResetPinDialog {
  protected readonly data = inject<SellerTerminalSummaryRow>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<ResetPinDialog>);
  private readonly api = inject(SellerTerminalApi);
  private readonly snackBar = inject(MatSnackBar);
  private readonly clipboard = inject(Clipboard);
  private readonly fb = inject(FormBuilder);

  readonly state = signal<DialogState>('confirming');
  readonly result = signal<ResetSellerTerminalPinResponse | null>(null);
  readonly errorMessage = signal<string | null>(null);
  readonly pinCopied = signal(false);

  readonly form = this.fb.group({
    reason: [null as PinResetReason | null, Validators.required],
  });

  submit(): void {
    if (this.form.invalid || this.state() === 'submitting') return;
    this.state.set('submitting');
    this.errorMessage.set(null);

    this.api
      .resetPin(this.data.id.value, { reason: this.form.controls.reason.value! })
      .subscribe({
        next: res => {
          this.result.set(res);
          this.state.set('success');
        },
        error: (err: unknown) => {
          const pd = (err as { error?: { title?: string; detail?: string } })?.error;
          this.errorMessage.set(pd?.detail ?? pd?.title ?? 'Erreur lors de la réinitialisation.');
          this.state.set('confirming');
        },
      });
  }

  copyPin(): void {
    const pin = this.result()?.temporaryPin;
    if (!pin) return;
    this.clipboard.copy(pin);
    this.pinCopied.set(true);
    setTimeout(() => this.pinCopied.set(false), 2000);
  }

  close(): void {
    this.result.set(null);
    this.dialogRef.close({ reload: true });
  }
}
