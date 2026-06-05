import { Component, inject } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { AuthSessionService } from '../../../core/auth/auth-session.service';
import { LanguageSwitcherComponent } from '../../../core/i18n';
import { AppRuntimeStore } from '../../../core/runtime';

@Component({
  imports: [LanguageSwitcherComponent, RouterLink, RouterOutlet, TranslatePipe],
  selector: 'tch-private-shell-page',
  template: `
    <div class="private-shell">
      <header class="top-app-bar">
        <a routerLink="/public" class="brand">Tchalanet</a>
        <div class="utilities">
          <tch-language-switcher />
          <button type="button" (click)="logout()">
            {{ 'dashboard.actions.logout' | translate }}
          </button>
        </div>
      </header>

      <div class="workspace">
        <nav class="side-nav" aria-label="Private navigation">
          <a routerLink="/app/cashier">{{ 'dashboard.titles.cashier' | translate }}</a>
          <a routerLink="/app/admin">{{ 'dashboard.titles.admin' | translate }}</a>
          <a routerLink="/app/platform">{{ 'dashboard.titles.platform' | translate }}</a>
        </nav>

        <main class="content">
          <router-outlet />
        </main>
      </div>
    </div>
  `,
  styles: [
    `
      .private-shell {
        min-height: 100vh;
      }

      .top-app-bar {
        align-items: center;
        border-bottom: 1px solid var(--tch-color-border, #d8dee6);
        display: flex;
        justify-content: space-between;
        min-height: 4rem;
        padding: 0 1.5rem;
      }

      .brand {
        color: inherit;
        font-weight: 700;
        text-decoration: none;
      }

      .utilities {
        align-items: center;
        display: flex;
        flex-wrap: wrap;
        gap: 0.75rem;
        justify-content: flex-end;
      }

      .workspace {
        display: grid;
        grid-template-columns: minmax(12rem, 16rem) 1fr;
        min-height: calc(100vh - 4rem);
      }

      .side-nav {
        border-right: 1px solid var(--tch-color-border, #d8dee6);
        display: flex;
        flex-direction: column;
        gap: 0.5rem;
        padding: 1.5rem;
      }

      .side-nav a {
        color: inherit;
        text-decoration: none;
      }

      .content {
        min-width: 0;
      }

      @media (max-width: 720px) {
        .top-app-bar {
          align-items: flex-start;
          flex-direction: column;
          gap: 0.75rem;
          padding-block: 1rem;
        }

        .workspace {
          grid-template-columns: 1fr;
        }

        .side-nav {
          border-bottom: 1px solid var(--tch-color-border, #d8dee6);
          border-right: 0;
          flex-direction: row;
          flex-wrap: wrap;
        }
      }
    `,
  ],
})
export class PrivateShellPage {
  private readonly auth = inject(AuthSessionService);
  private readonly runtime = inject(AppRuntimeStore);

  constructor() {
    this.runtime.initPrivateRuntime();
  }

  logout(): void {
    void this.auth.logout();
  }
}
