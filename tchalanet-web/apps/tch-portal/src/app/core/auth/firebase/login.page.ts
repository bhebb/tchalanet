import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
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
    MatButtonModule,
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
export class LoginPage implements OnInit {
  email = '';
  password = '';
  remember = true;

  readonly loading = signal(false);
  readonly errorKey = signal<string | null>(null);
  readonly infoKey = signal<string | null>(null);
  readonly passwordVisible = signal(false);

  private readonly authSession = inject(AuthSessionService);
  private readonly router = inject(Router);

  async ngOnInit(): Promise<void> {
    this.loading.set(true);
    this.errorKey.set(null);
    try {
      const session = await this.authSession.completePasswordlessLogin();
      if (session?.authenticated) {
        await this.router.navigateByUrl('/app');
      }
    } catch {
      this.errorKey.set('auth.login.errors.emailLinkFailed');
    } finally {
      this.loading.set(false);
    }
  }

  togglePasswordVisibility(): void {
    this.passwordVisible.update(visible => !visible);
  }

  async submit(): Promise<void> {
    this.loading.set(true);
    this.errorKey.set(null);
    this.infoKey.set(null);
    const usePasswordlessLogin = !this.password.trim();

    try {
      if (usePasswordlessLogin) {
        await this.authSession.sendPasswordlessLoginLink(this.email);
        this.infoKey.set('auth.login.form.emailLinkSent');
        return;
      }

      const session = await this.authSession.login(this.email, this.password, this.remember);

      if (!session.authenticated) {
        this.errorKey.set('auth.login.errors.accessDenied');
        return;
      }

      await this.router.navigateByUrl('/app');
    } catch {
      this.errorKey.set(
        usePasswordlessLogin
          ? 'auth.login.errors.emailLinkFailed'
          : 'auth.login.errors.invalidCredentials',
      );
    } finally {
      this.loading.set(false);
    }
  }
}
