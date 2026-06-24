import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

import { AccountActivationApi } from '../../../features/private/account/data-access/account-activation-api.service';

const passwordsMatch: ValidatorFn = (group: AbstractControl): ValidationErrors | null => {
  const pw = group.get('newPassword')?.value;
  const confirm = group.get('confirmPassword')?.value;
  return pw && confirm && pw !== confirm ? { passwordsMismatch: true } : null;
};

@Component({
  selector: 'tch-forgot-password-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
  ],
  templateUrl: './forgot-password.page.html',
  styleUrls: ['./forgot-password.page.scss'],
})
export class ForgotPasswordPage {
  private readonly api = inject(AccountActivationApi);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  readonly submitting = signal(false);
  readonly done = signal(false);
  readonly error = signal<string | null>(null);
  readonly passwordVisible = signal(false);

  readonly form = this.fb.group(
    {
      email: ['', [Validators.required, Validators.email]],
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', Validators.required],
    },
    { validators: passwordsMatch },
  );

  togglePasswordVisibility(): void {
    this.passwordVisible.update(v => !v);
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) return;
    const { email, newPassword } = this.form.value;
    this.submitting.set(true);
    this.error.set(null);
    this.api.resetPassword({ email: email!, newPassword: newPassword! }).subscribe({
      next: () => {
        this.submitting.set(false);
        this.done.set(true);
      },
      error: (err: { error?: { title?: string } }) => {
        this.error.set(err?.error?.title ?? 'auth.forgotPassword.error');
        this.submitting.set(false);
      },
    });
  }
}
