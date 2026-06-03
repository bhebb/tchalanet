import { Component, computed, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { AuthSessionService } from '../../core/auth/auth-session.service';
import { I18nFacade, LanguageSwitcherComponent } from '../../core/i18n';
import { RuntimeSettingsStore } from '../../core/settings';

@Component({
  imports: [RouterLink, TranslatePipe, LanguageSwitcherComponent],
  selector: 'tch-role-dashboard-page',
  template: `
    <section class="page">
      <tch-language-switcher />
      <h1>{{ titleKey() | translate }}</h1>

      <dl>
        <dt>{{ 'dashboard.fields.user' | translate }}</dt>
        <dd>{{ session().displayName || session().username || ('common.unknown' | translate) }}</dd>
        <dt>{{ 'dashboard.fields.tenant' | translate }}</dt>
        <dd>
          {{ session().tenantCode || session().tenantId || ('dashboard.tenantMissing' | translate) }}
        </dd>
        <dt>{{ 'dashboard.fields.roles' | translate }}</dt>
        <dd>{{ roles() || ('dashboard.rolesEmpty' | translate) }}</dd>
        <dt>{{ 'dashboard.fields.settings' | translate }}</dt>
        <dd>{{ settingsState() }}</dd>
      </dl>

      <div class="actions">
        <a routerLink="/public">{{ 'dashboard.actions.publicHome' | translate }}</a>
        <button type="button" (click)="logout()">
          {{ 'dashboard.actions.logout' | translate }}
        </button>
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
  private readonly i18n = inject(I18nFacade);
  private readonly settings = inject(RuntimeSettingsStore);

  readonly session = this.auth.session;
  readonly titleKey = computed(() => this.route.snapshot.data['titleKey'] as string);
  readonly roles = computed(() => this.session().roles.join(', '));
  readonly settingsState = this.settings.loadState;

  constructor() {
    this.i18n.init();
    this.settings.loadPrivateSettings();
  }

  logout(): void {
    void this.auth.logout();
  }
}
