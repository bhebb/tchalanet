import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Router, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { TchRuntimeConfigStore } from '@tch/shared-config';

import { TCH_BRAND_ASSETS } from '@tch/shared-assets';

import { AuthSessionService } from '../auth-session.service';
import { AuthRedirectService } from '../auth-redirect.service';
import { AUTH_CLIENT } from '../auth-client';
import { LanguageSwitcher } from '@tch/core/i18n';

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
  readonly errorKey = signal<string | null>(null);
  readonly infoKey = signal<string | null>(null);
  readonly passwordVisible = signal(false);
  readonly brandLogo = TCH_BRAND_ASSETS.logo;
  readonly brandLogoInverse = TCH_BRAND_ASSETS.logoInverse;

  private readonly authSession = inject(AuthSessionService);
  private readonly authRedirect = inject(AuthRedirectService);
  private readonly authClient = inject(AUTH_CLIENT);
  private readonly router = inject(Router);
  private readonly runtimeConfig = inject(TchRuntimeConfigStore);

  // sessionStorage key used to detect guard-bounce loops.
  // Set just before redirecting; if we land back on /login within the window, skip redirect.
  private static readonly RESTORE_TS_KEY = 'tch-session-restore-ts';

  // Must exceed the backend bootstrap timeout (AUTH_OPERATION_TIMEOUT_MS = 15s):
  // the guard bounce goes through refreshSession() → /runtime/private, which can take
  // up to 15s to fail. A shorter window closes before the bounce returns, so the loop
  // would never be detected. 20s leaves a safety margin over the 15s timeout.
  private static readonly RESTORE_WINDOW_MS = 20_000;

  ngOnInit(): void {
    void this.redirectRestoredSession();
  }

  private async redirectRestoredSession(): Promise<void> {
    // Guard-bounce detection: if the last redirect attempt was within the window and
    // we're back on /login, the backend rejected the session — don't loop again.
    const lastAttempt = Number(sessionStorage.getItem(LoginPage.RESTORE_TS_KEY) ?? 0);
    if (Date.now() - lastAttempt < LoginPage.RESTORE_WINDOW_MS) {
      sessionStorage.removeItem(LoginPage.RESTORE_TS_KEY);
      return;
    }

    try {
      if (!(await withTimeout(this.authClient.isAuthenticated(), 2_000))) {
        return;
      }
      const route = this.localAuthenticatedEntryRoute();
      if (!route) return;
      sessionStorage.setItem(LoginPage.RESTORE_TS_KEY, String(Date.now()));
      await this.router.navigateByUrl(route);
    } catch {
      sessionStorage.removeItem(LoginPage.RESTORE_TS_KEY);
      // Keep the form immediately usable when provider restoration is slow.
    }
  }

  private localAuthenticatedEntryRoute(): string | null {
    const appId = this.runtimeConfig.config().appId;
    if (appId === 'admin-portal') return '/app/admin';
    if (appId === 'platform-portal') return '/app/platform';
    return null;
  }

  togglePasswordVisibility(): void {
    this.passwordVisible.update(visible => !visible);
  }

  async submit(): Promise<void> {
    this.loading.set(true);
    this.errorKey.set(null);
    this.infoKey.set(null);

    try {
      const session = await this.authSession.login(this.email, this.password);

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

function withTimeout<T>(promise: Promise<T>, timeoutMs: number): Promise<T> {
  return new Promise<T>((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error('auth.restore.timeout')), timeoutMs);
    promise.then(
      value => {
        clearTimeout(timer);
        resolve(value);
      },
      error => {
        clearTimeout(timer);
        reject(error);
      },
    );
  });
}
