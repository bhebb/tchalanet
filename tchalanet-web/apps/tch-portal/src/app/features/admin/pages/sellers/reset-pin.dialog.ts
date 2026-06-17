import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';

const PIN_LENGTH = 4;

export interface ResetPinDialogData {
  readonly sellerName: string;
}

export interface ResetPinDialogResult {
  readonly pin: string;
}

@Component({
  selector: 'tch-reset-pin-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>Réinitialiser le PIN</h2>
    <mat-dialog-content>
      <p class="reset-pin__intro">
        Saisissez le nouveau PIN pour <strong>{{ data.sellerName }}</strong>.
      </p>
      <div class="pin-inputs" role="group" aria-label="Nouveau PIN">
        @for (i of indices; track i) {
          <input
            class="pin-box"
            [class.pin-box--error]="hasError()"
            type="text"
            inputmode="numeric"
            maxlength="1"
            [id]="'rpin-' + i"
            [value]="digits()[i]"
            (input)="onInput($event, i)"
            (keydown)="onKeydown($event, i)"
            (paste)="onPaste($event)"
            autocomplete="off"
          />
        }
      </div>
      @if (hasError()) {
        <p class="reset-pin__error" role="alert">Le PIN doit contenir 4 chiffres.</p>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Annuler</button>
      <button mat-flat-button (click)="confirm()">Confirmer</button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      .reset-pin__intro {
        margin: 0 0 1.25rem;
        color: var(--tch-color-on-surface-variant);
      }

      .pin-inputs {
        display: flex;
        gap: 0.75rem;
      }

      .pin-box {
        width: 3rem;
        height: 3.5rem;
        text-align: center;
        font-size: 1.5rem;
        font-weight: 700;
        border: 2px solid var(--tch-color-outline-variant);
        border-radius: var(--tch-radius-md, 8px);
        background: var(--tch-color-surface-container-low);
        color: var(--tch-color-on-surface);
        outline: none;
        transition: border-color 150ms;

        &:focus {
          border-color: var(--tch-color-primary);
        }
      }

      .pin-box--error {
        border-color: var(--tch-color-error);
      }

      .reset-pin__error {
        margin: 0.5rem 0 0;
        font-size: 0.75rem;
        color: var(--tch-color-error);
      }
    `,
  ],
})
export class ResetPinDialog {
  protected readonly data = inject<ResetPinDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<ResetPinDialog, ResetPinDialogResult>);

  readonly indices = Array.from({ length: PIN_LENGTH }, (_, i) => i);
  readonly digits = signal<string[]>(Array(PIN_LENGTH).fill(''));
  readonly hasError = signal(false);

  onInput(event: Event, index: number): void {
    const el = event.target as HTMLInputElement;
    const digit = el.value.replace(/\D/g, '').slice(-1);
    const d = [...this.digits()];
    d[index] = digit;
    this.digits.set(d);
    this.hasError.set(false);
    if (digit && index < PIN_LENGTH - 1) {
      document.getElementById(`rpin-${index + 1}`)?.focus();
    }
  }

  onKeydown(event: KeyboardEvent, index: number): void {
    if (event.key === 'Backspace' && !this.digits()[index] && index > 0) {
      document.getElementById(`rpin-${index - 1}`)?.focus();
    }
  }

  onPaste(event: ClipboardEvent): void {
    event.preventDefault();
    const text = event.clipboardData?.getData('text') ?? '';
    const filled = Array(PIN_LENGTH).fill('');
    text.replace(/\D/g, '').slice(0, PIN_LENGTH).split('').forEach((d, i) => (filled[i] = d));
    this.digits.set(filled);
  }

  confirm(): void {
    const pin = this.digits().join('');
    if (pin.length < PIN_LENGTH) {
      this.hasError.set(true);
      return;
    }
    this.dialogRef.close({ pin });
  }
}
