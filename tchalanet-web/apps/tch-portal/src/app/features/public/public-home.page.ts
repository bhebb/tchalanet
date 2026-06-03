import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AuthSessionService } from '../../core/auth/auth-session.service';

@Component({
  imports: [RouterLink],
  selector: 'tch-public-home-page',
  template: `
    <section class="page">
      <h1>Tchalanet Web</h1>
      <p>Public route is available without login.</p>

      <div class="actions">
        <button type="button" (click)="login()">Login</button>
        @if (authenticated()) {
          <button type="button" (click)="logout()">Logout</button>
        }
      </div>

      <nav>
        <a routerLink="/app/cashier">Cashier</a>
        <a routerLink="/app/admin">Tenant admin</a>
        <a routerLink="/app/platform">Platform</a>
      </nav>
    </section>
  `,
  styles: [
    `
      .page {
        display: grid;
        gap: 1rem;
        max-width: 720px;
        padding: 2rem;
      }

      .actions,
      nav {
        display: flex;
        flex-wrap: wrap;
        gap: 0.75rem;
      }
    `,
  ],
})
export class PublicHomePage {
  private readonly auth = inject(AuthSessionService);

  readonly authenticated = computed(() => this.auth.session().authenticated);

  login(): void {
    void this.auth.login();
  }

  logout(): void {
    void this.auth.logout();
  }
}
