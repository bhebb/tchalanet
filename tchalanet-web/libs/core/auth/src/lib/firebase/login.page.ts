import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { TCH_BRAND_ASSETS } from '@tch/shared-assets';

import { AuthSessionService } from '../auth-session.service';
import { AuthRedirectService } from '../auth-redirect.service';
import { LanguageSwitcher } from '@tch/core/i18n';

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
  readonly brandLogo = TCH_BRAND_ASSETS.logo;
  readonly brandLogoInverse = TCH_BRAND_ASSETS.logoInverse;

  private readonly authSession = inject(AuthSessionService);
  private readonly authRedirect = inject(AuthRedirectService);

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

      await this.authRedirect.navigateAfterLogin(session);
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
