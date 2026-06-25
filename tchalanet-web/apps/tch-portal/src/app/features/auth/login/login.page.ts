import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Router } from '@angular/router';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { AuthSessionService } from '../../../core/auth/auth-session.service';
import { LanguageSwitcher } from '../../../core/i18n';

@Component({
  standalone: true,
  selector: 'tch-login-page',
  imports: [
    FormsModule,
    LanguageSwitcher,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    RouterLink,
    TranslatePipe,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './login.page.html',
  styleUrl: './login.page.scss',
})
export class LoginPage implements OnInit {
  email = '';
  password = '';

  readonly loading = signal(false);
  readonly checkingSession = signal(true);
  readonly errorKey = signal<string | null>(null);
  readonly passwordVisible = signal(false);

  private readonly authSession = inject(AuthSessionService);
  private readonly router = inject(Router);

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
    this.passwordVisible.update(visible => !visible);
  }

  submitFromEnter(event: Event, form: NgForm): void {
    event.preventDefault();
    void this.submit(form);
  }

  async submit(form: NgForm): Promise<void> {
    if (this.loading() || form.invalid) {
      form.control.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorKey.set(null);

    try {
      const session = await this.authSession.login(this.email, this.password);

      if (!session.authenticated) {
        this.errorKey.set('auth.login.errors.accessDenied');
        return;
      }

      await this.router.navigateByUrl('/app');
    } catch {
      this.errorKey.set('auth.login.errors.invalidCredentials');
    } finally {
      this.loading.set(false);
    }
  }
}
