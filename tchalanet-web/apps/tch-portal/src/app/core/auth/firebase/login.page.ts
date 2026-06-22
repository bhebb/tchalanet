import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Router } from '@angular/router';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { AuthSessionService } from '../auth-session.service';
import { LanguageSwitcher } from '../../i18n';

@Component({
  standalone: true,
  selector: 'tch-login-page',
  imports: [
    FormsModule,
    LanguageSwitcher,
    MatCheckboxModule,
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
export class LoginPage {
  email = '';
  password = '';
  remember = true;

  readonly loading = signal(false);
  readonly errorKey = signal<string | null>(null);
  readonly infoKey = signal<string | null>(null);
  readonly passwordVisible = signal(false);

  private readonly authSession = inject(AuthSessionService);
  private readonly router = inject(Router);

  togglePasswordVisibility(): void {
    this.passwordVisible.update(visible => !visible);
  }

  async submit(): Promise<void> {
    this.loading.set(true);
    this.errorKey.set(null);
    this.infoKey.set(null);

    try {
      const session = await this.authSession.login(this.email, this.password, this.remember);

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

  async forgotPassword(): Promise<void> {
    if (!this.email.trim()) {
      this.errorKey.set('auth.login.errors.emailRequired');
      return;
    }

    this.loading.set(true);
    this.errorKey.set(null);
    this.infoKey.set(null);

    try {
      await this.authSession.sendPasswordResetEmail(this.email);
      this.infoKey.set('auth.login.form.resetEmailSent');
    } catch {
      this.errorKey.set('auth.login.errors.emailLinkFailed');
    } finally {
      this.loading.set(false);
    }
  }
}
