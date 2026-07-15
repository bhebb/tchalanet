import { ChangeDetectionStrategy, Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

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
export class LoginPage implements OnInit, OnDestroy {
  identifier = '';
  password = '';

  readonly loading = signal(false);
  readonly errorKey = signal<string | null>(null);
  readonly infoKey = signal<string | null>(null);
  readonly passwordVisible = signal(false);
  readonly installHintVisible = signal(false);
  readonly installPromptAvailable = signal(false);
  readonly brandLogo = TCH_BRAND_ASSETS.logo;
  readonly brandLogoInverse = TCH_BRAND_ASSETS.logoInverse;

  private readonly authSession = inject(AuthSessionService);
  private readonly authRedirect = inject(AuthRedirectService);
  private readonly authClient = inject(AUTH_CLIENT);

  // sessionStorage key used to detect guard-bounce loops.
  // Set just before redirecting; if we land back on /login within the window, skip redirect.
  private static readonly RESTORE_TS_KEY = 'tch-session-restore-ts';

  // Must exceed the backend bootstrap timeout (AUTH_OPERATION_TIMEOUT_MS = 15s):
  // the guard bounce goes through refreshSession() → /runtime/private, which can take
  // up to 15s to fail. A shorter window closes before the bounce returns, so the loop
  // would never be detected. 20s leaves a safety margin over the 15s timeout.
  private static readonly RESTORE_WINDOW_MS = 20_000;
  private installPromptEvent: BeforeInstallPromptEvent | null = null;

  private readonly handleBeforeInstallPrompt = (event: Event): void => {
    event.preventDefault();
    this.installPromptEvent = event as BeforeInstallPromptEvent;
    this.installPromptAvailable.set(true);
    this.installHintVisible.set(!this.isStandaloneMode());
  };

  private readonly handleAppInstalled = (): void => {
    this.installPromptEvent = null;
    this.installPromptAvailable.set(false);
    this.installHintVisible.set(false);
  };

  ngOnInit(): void {
    this.setupInstallHint();
    void this.redirectRestoredSession();
  }

  ngOnDestroy(): void {
    const windowRef = globalThis.window;
    windowRef?.removeEventListener('beforeinstallprompt', this.handleBeforeInstallPrompt);
    windowRef?.removeEventListener('appinstalled', this.handleAppInstalled);
  }

  private async redirectRestoredSession(): Promise<void> {
    // Guard-bounce detection: if the last redirect attempt was within the window and
    // we're back on /login, the backend rejected the session — don't loop again.
    const storage = this.sessionStorage();
    const lastAttempt = Number(storage?.getItem(LoginPage.RESTORE_TS_KEY) ?? 0);
    if (Date.now() - lastAttempt < LoginPage.RESTORE_WINDOW_MS) {
      storage?.removeItem(LoginPage.RESTORE_TS_KEY);
      return;
    }

    try {
      if (!(await withTimeout(this.authClient.isAuthenticated(), 2_000))) {
        return;
      }
      const session = await this.authSession.refreshSession(true);
      if (!session.authenticated) return;
      storage?.setItem(LoginPage.RESTORE_TS_KEY, String(Date.now()));
      await this.authRedirect.navigateAfterLogin(session);
    } catch {
      storage?.removeItem(LoginPage.RESTORE_TS_KEY);
      // Keep the form immediately usable when provider restoration is slow.
    }
  }

  private sessionStorage(): Storage | null {
    return globalThis.window?.sessionStorage ?? null;
  }

  togglePasswordVisibility(): void {
    this.passwordVisible.update(visible => !visible);
  }

  async installApp(): Promise<void> {
    this.errorKey.set(null);
    this.infoKey.set(null);

    if (!this.installPromptEvent) {
      this.infoKey.set(this.isAppleTouchDevice() ? 'auth.login.install.iosHint' : 'auth.login.install.browserHint');
      return;
    }

    const promptEvent = this.installPromptEvent;
    this.installPromptEvent = null;
    this.installPromptAvailable.set(false);

    await promptEvent.prompt();
    const choice = await promptEvent.userChoice;
    if (choice.outcome === 'accepted') {
      this.installHintVisible.set(false);
    }
  }

  async submit(): Promise<void> {
    this.loading.set(true);
    this.errorKey.set(null);
    this.infoKey.set(null);

    try {
      const session = await this.authSession.login(this.identifier, this.password);

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
    if (!this.identifier.trim()) {
      this.errorKey.set('auth.login.errors.emailRequired');
      return;
    }

    this.loading.set(true);
    this.errorKey.set(null);
    this.infoKey.set(null);

    try {
      await this.authSession.sendPasswordResetEmail(this.identifier);
      this.infoKey.set('auth.login.form.resetEmailSent');
    } catch {
      this.errorKey.set('auth.login.errors.emailLinkFailed');
    } finally {
      this.loading.set(false);
    }
  }

  private setupInstallHint(): void {
    const windowRef = globalThis.window;
    if (!windowRef || this.isStandaloneMode()) return;

    this.installHintVisible.set(this.isMobileViewport());
    windowRef.addEventListener('beforeinstallprompt', this.handleBeforeInstallPrompt);
    windowRef.addEventListener('appinstalled', this.handleAppInstalled);
  }

  private isMobileViewport(): boolean {
    return globalThis.window?.matchMedia?.('(max-width: 599px)').matches ?? false;
  }

  private isStandaloneMode(): boolean {
    const windowRef = globalThis.window;
    const navigatorRef = windowRef?.navigator as NavigatorWithStandalone | undefined;
    return windowRef?.matchMedia?.('(display-mode: standalone)').matches === true || navigatorRef?.standalone === true;
  }

  private isAppleTouchDevice(): boolean {
    return /iphone|ipad|ipod/i.test(globalThis.window?.navigator.userAgent ?? '');
  }
}

interface BeforeInstallPromptEvent extends Event {
  readonly userChoice: Promise<{ outcome: 'accepted' | 'dismissed'; platform: string }>;
  prompt(): Promise<void>;
}

interface NavigatorWithStandalone extends Navigator {
  readonly standalone?: boolean;
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
