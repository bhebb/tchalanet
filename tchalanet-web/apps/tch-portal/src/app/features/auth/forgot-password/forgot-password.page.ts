import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

import { AuthSessionService } from '@tch/core/auth';
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
export class ForgotPasswordPage implements OnInit {
  private readonly api = inject(AccountActivationApi);
  private readonly authSession = inject(AuthSessionService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  readonly checkingSession = signal(true);
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

  async ngOnInit(): Promise<void> {
    try {
      const session = await this.authSession.refreshSession();
      if (session.authenticated) {
        await this.router.navigateByUrl('/app');
      }
    } finally {
      this.checkingSession.set(false);
    }
  }

  togglePasswordVisibility(): void {
    this.passwordVisible.update(v => !v);
  }

  submitFromEnter(event: Event): void {
    event.preventDefault();
    this.submit();
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }
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
