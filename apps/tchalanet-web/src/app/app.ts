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
    auth.wireOidcEvents(); // pour que silent refresh réhydrate tes signals
  }
}
