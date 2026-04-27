import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';

import { AuthService } from '@tchl/shared/auth';

@Component({
  imports: [RouterModule],
  standalone: true,
  selector: 'tch-root',
  template: `<router-outlet></router-outlet>`,
  styles: ``,
})
export class App {
  protected title = 'tchalanet';

  constructor(auth: AuthService) {
    try {
      auth.wireOidcEvents(); // pour que silent refresh réhydrate les signals
    } catch (err) {
      // Avoid breaking bootstrap if auth wiring throws — log for debugging.
      // eslint-disable-next-line no-console
      console.error('Auth wiring failed during App construction', err);
    }
  }
}
