import { ChangeDetectionStrategy, Component, output, signal } from '@angular/core';
import { FormField, form, required } from '@angular/forms/signals';

export interface TchLoginCredentials {
  readonly username: string;
  readonly password: string;
}

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormField],
  selector: 'tch-login-page',
  templateUrl: './tch-login.page.html',
  styleUrl: './tch-login.page.scss',
})
export class TchLoginPage {
  readonly submitted = output<TchLoginCredentials>();

  protected readonly loginModel = signal<TchLoginCredentials>({
    username: '',
    password: '',
  });

  protected readonly loginForm = form(this.loginModel, path => {
    required(path.username, { message: 'auth.login.validation.usernameRequired' });
    required(path.password, { message: 'auth.login.validation.passwordRequired' });
  });

  protected submitLogin(event: SubmitEvent): void {
    event.preventDefault();

    if (this.loginForm().invalid()) {
      return;
    }

    this.submitted.emit(this.loginModel());
  }
}
