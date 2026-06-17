import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import {
  AdminPageHeader,
  AdminFormShell,
  AdminFormSection,
  AdminFormActions,
} from '@tch/ui/components';
import { finalize } from 'rxjs';

import { SellerTerminalAdminApi } from '../../seller-terminal-admin.api.service';

const PIN_LENGTH = 4;

@Component({
  selector: 'tch-admin-seller-new-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    TranslatePipe,
    RouterLink,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    AdminPageHeader,
    AdminFormShell,
    AdminFormSection,
    AdminFormActions,
  ],
  templateUrl: './admin-seller-new.page.html',
  styleUrl: './admin-seller-new.page.scss',
})
export class AdminSellerNewPage {
  protected readonly router = inject(Router);
  private readonly api = inject(SellerTerminalAdminApi);
  private readonly fb = inject(FormBuilder);

  readonly pinIndices = Array.from({ length: PIN_LENGTH }, (_, i) => i);
  readonly pinDigits = signal<string[]>(Array(PIN_LENGTH).fill(''));
  readonly pinError = signal(false);
  readonly pending = signal(false);
  readonly submitError = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    displayName:    ['', Validators.required],
    phoneNumber:    ['', Validators.required],
    firstName:      [''],
    lastName:       [''],
    terminalCode:   ['', Validators.required],
    commissionRate: [null as number | null, [Validators.min(0), Validators.max(100)]],
  });

  isPinComplete(): boolean {
    return this.pinDigits().every(d => d.length === 1);
  }

  onPinInput(event: Event, index: number): void {
    const input = event.target as HTMLInputElement;
    const digit = input.value.replace(/\D/g, '').slice(-1);
    const digits = [...this.pinDigits()];
    digits[index] = digit;
    this.pinDigits.set(digits);
    this.pinError.set(false);

    if (digit && index < PIN_LENGTH - 1) {
      document.getElementById(`pin-${index + 1}`)?.focus();
    }
  }

  onPinKeydown(event: KeyboardEvent, index: number): void {
    if (event.key === 'Backspace' && !this.pinDigits()[index] && index > 0) {
      document.getElementById(`pin-${index - 1}`)?.focus();
    }
  }

  onPinPaste(event: ClipboardEvent): void {
    event.preventDefault();
    const text = event.clipboardData?.getData('text') ?? '';
    const digits = text.replace(/\D/g, '').slice(0, PIN_LENGTH).split('');
    const filled = [...Array(PIN_LENGTH).fill('')];
    digits.forEach((d, i) => (filled[i] = d));
    this.pinDigits.set(filled);
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (!this.isPinComplete()) {
      this.pinError.set(true);
      return;
    }

    this.pending.set(true);
    this.submitError.set(null);

    const { displayName, phoneNumber, firstName, lastName, terminalCode, commissionRate } = this.form.getRawValue();

    this.api
      .create({
        displayName,
        phoneNumber: phoneNumber || undefined,
        firstName: firstName || undefined,
        lastName: lastName || undefined,
        terminalCode,
        commissionRate: commissionRate ?? undefined,
        initialPin: this.pinDigits().join(''),
      })
      .pipe(finalize(() => this.pending.set(false)))
      .subscribe({
        next: id => this.router.navigate(['..', id], { relativeTo: null }),
        error: () => this.submitError.set('Une erreur est survenue. Veuillez réessayer.'),
      });
  }
}
