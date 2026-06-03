import { Component, computed, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { AuthSessionService } from '../../core/auth/auth-session.service';

@Component({
  imports: [RouterLink],
  selector: 'tch-role-dashboard-page',
  template: `
    <section class="page">
      <h1>{{ title() }}</h1>

      <dl>
        <dt>User</dt>
        <dd>{{ session().displayName || session().username || 'Unknown' }}</dd>
        <dt>Tenant</dt>
        <dd>{{ session().tenantCode || session().tenantId || 'Not detected' }}</dd>
        <dt>Roles</dt>
        <dd>{{ roles() }}</dd>
      </dl>

      <div class="actions">
        <a routerLink="/public">Public home</a>
        <button type="button" (click)="logout()">Logout</button>
      </div>
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

      dl {
        display: grid;
        grid-template-columns: max-content 1fr;
        gap: 0.5rem 1rem;
      }

      dt {
        font-weight: 700;
      }

      .actions {
        display: flex;
        gap: 0.75rem;
      }
    `,
  ],
})
export class RoleDashboardPage {
  private readonly route = inject(ActivatedRoute);
  private readonly auth = inject(AuthSessionService);

  readonly session = this.auth.session;
  readonly title = computed(() => this.route.snapshot.data['title'] as string);
  readonly roles = computed(() => this.session().roles.join(', ') || 'No role detected');

  logout(): void {
    void this.auth.logout();
  }
}
