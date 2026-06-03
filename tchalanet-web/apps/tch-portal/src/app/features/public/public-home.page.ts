import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { AuthSessionService } from '../../core/auth/auth-session.service';
import { LanguageSwitcherComponent } from '../../core/i18n';
import { AppRuntimeStore } from '../../core/runtime';
import { ThemeSwitcherComponent } from '../../core/theme';

@Component({
  imports: [RouterLink, TranslatePipe, LanguageSwitcherComponent, ThemeSwitcherComponent],
  selector: 'tch-public-home-page',
  template: `
    <section class="page">
      <tch-language-switcher />
      <tch-theme-switcher />
      <h1>{{ 'home.hero.title' | translate }}</h1>
      <p>{{ 'home.hero.subtitle' | translate }}</p>
      <p>{{ 'public.home.settings' | translate }}: {{ settingsState() }}</p>
      <p>Runtime: {{ runtimeState() }} / {{ currentLanguage() }} / {{ currentTheme().activePresetKey }}</p>
      <p>
        {{ 'public.home.demoFlag' | translate }}:
        {{ (demoFlagEnabled() ? 'common.enabled' : 'common.disabled') | translate }}
      </p>

      <div class="actions">
        <button type="button" (click)="login()">
          {{ 'home.hero.cta' | translate }}
        </button>
        @if (authenticated()) {
          <button type="button" (click)="logout()">
            {{ 'public.home.logout' | translate }}
          </button>
        }
      </div>

      <nav>
        <a routerLink="/public">{{ 'home.nav.results' | translate }}</a>
        <a routerLink="/public">{{ 'home.nav.check_ticket' | translate }}</a>
        <a routerLink="/app/cashier">{{ 'public.home.nav.cashier' | translate }}</a>
        <a routerLink="/app/admin">{{ 'public.home.nav.admin' | translate }}</a>
        <a routerLink="/app/platform">{{ 'public.home.nav.platform' | translate }}</a>
      </nav>
    </section>
  `,
  styles: [
    `
      .page {
        color: var(--tch-color-foreground);
        background: var(--tch-color-background);
        display: grid;
        gap: 1rem;
        max-width: 720px;
        padding: 2rem;
      }

      h1 {
        color: var(--tch-color-primary);
      }

      button,
      a {
        color: var(--tch-color-primary);
      }

      button {
        border: 1px solid var(--tch-color-primary);
        border-radius: var(--tch-radius-control);
        background: var(--tch-color-primary);
        color: var(--tch-color-primary-contrast);
        padding: 0.5rem 0.75rem;
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
  private readonly runtime = inject(AppRuntimeStore);

  readonly authenticated = computed(() => this.auth.session().authenticated);
  readonly runtimeState = this.runtime.state;
  readonly currentLanguage = this.runtime.currentLanguage;
  readonly currentTheme = this.runtime.currentTheme;
  readonly settingsState = this.runtime.settingsState;
  readonly demoFlagEnabled = computed(() =>
    this.runtime.isFeatureEnabled('web.public.demo_enabled', false),
  );

  login(): void {
    void this.auth.login();
  }

  logout(): void {
    void this.auth.logout();
  }
}
