import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { AuthSessionService } from '../../../core/auth/auth-session.service';
import { LanguageSwitcherComponent } from '../../../core/i18n';
import { ThemeSwitcherComponent } from '../../../core/theme';
import { PageShell, UserRole } from '../../../shared/types';
import { LabelPipe } from '../label.pipe';

const DASHBOARD_BY_ROLE: ReadonlyArray<readonly [UserRole, string]> = [
  ['SUPER_ADMIN', '/app/platform'],
  ['TENANT_ADMIN', '/app/admin'],
  ['CASHIER', '/app/cashier'],
];

/**
 * Lightweight public shell: renders a top app bar and footer from the `shell` config, with the
 * existing language/theme switchers, and projects the page body. Not a generic shell engine —
 * just header + footer + content per the design's composition (top app bar, body, simple footer).
 */
@Component({
  selector: 'tch-page-shell',
  imports: [RouterLink, TranslatePipe, LanguageSwitcherComponent, ThemeSwitcherComponent, LabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="shell__bar">
      <a class="shell__brand" routerLink="/public">{{ 'app.brand' | tchLabel }}</a>
      <nav class="shell__nav">
        @for (item of headerNav(); track item.path) {
          <a [routerLink]="item.path">{{ item.label_key | tchLabel }}</a>
        }
      </nav>
      <div class="shell__actions">
        <tch-language-switcher />
        <tch-theme-switcher />
        @if (authenticated()) {
          @if (dashboardLink(); as link) {
            <a class="shell__cta" [routerLink]="link">{{ 'app.nav.dashboard' | translate }}</a>
          }
          <button type="button" class="shell__btn" (click)="logout()">
            {{ 'public.home.logout' | translate }}
          </button>
        } @else {
          <button type="button" class="shell__cta" (click)="login()">
            {{ 'public.home.login' | translate }}
          </button>
        }
      </div>
    </header>

    <main class="shell__body">
      <ng-content />
    </main>

    <footer class="shell__footer">
      <nav class="shell__nav">
        @for (item of footerNav(); track item.path) {
          <a [routerLink]="item.path">{{ item.label_key | tchLabel }}</a>
        }
      </nav>
      <small>{{ 'app.footer.copyright' | tchLabel }}</small>
    </footer>
  `,
  styles: [
    `
      :host {
        display: grid;
        min-height: 100vh;
        grid-template-rows: auto 1fr auto;
        background: var(--tch-color-background, var(--mat-sys-background));
        color: var(--tch-color-foreground, var(--mat-sys-on-background));
      }
      .shell__bar,
      .shell__footer {
        display: flex;
        align-items: center;
        gap: 1rem;
        padding: 0.75rem 1.5rem;
        background: var(--tch-color-surface, var(--mat-sys-surface));
        border-bottom: 1px solid var(--tch-color-outline, var(--mat-sys-outline-variant));
      }
      .shell__footer {
        border-bottom: none;
        border-top: 1px solid var(--tch-color-outline, var(--mat-sys-outline-variant));
        justify-content: space-between;
      }
      .shell__brand {
        font-weight: 700;
        color: var(--tch-color-primary, var(--mat-sys-primary));
        text-decoration: none;
      }
      .shell__nav {
        display: flex;
        flex-wrap: wrap;
        gap: 1rem;
      }
      .shell__nav a {
        color: inherit;
        text-decoration: none;
      }
      .shell__actions {
        margin-left: auto;
        display: flex;
        gap: 0.5rem;
        align-items: center;
      }
      .shell__cta {
        padding: 0.4rem 0.85rem;
        border-radius: var(--tch-radius-control, 8px);
        background: var(--tch-color-primary, var(--mat-sys-primary));
        color: var(--tch-color-primary-contrast, var(--mat-sys-on-primary));
        text-decoration: none;
        font-weight: 600;
        border: 1px solid var(--tch-color-primary, var(--mat-sys-primary));
        cursor: pointer;
      }
      .shell__btn {
        padding: 0.4rem 0.85rem;
        border-radius: var(--tch-radius-control, 8px);
        background: transparent;
        color: var(--tch-color-primary, var(--mat-sys-primary));
        border: 1px solid var(--tch-color-outline, var(--mat-sys-outline-variant));
        cursor: pointer;
      }
    `,
  ],
})
export class PageShellComponent {
  private readonly auth = inject(AuthSessionService);

  readonly shell = input<PageShell>();

  readonly headerNav = computed(() => this.shell()?.header?.nav?.primary ?? []);
  readonly footerNav = computed(() => this.shell()?.footer?.nav?.primary ?? []);

  readonly authenticated = this.auth.authenticated;

  /** Route to the signed-in user's dashboard, picking the highest-privilege role they hold. */
  readonly dashboardLink = computed(() => {
    const roles = this.auth.session().roles;
    return DASHBOARD_BY_ROLE.find(([role]) => roles.includes(role))?.[1];
  });

  login(): void {
    void this.auth.login();
  }

  logout(): void {
    void this.auth.logout();
  }
}
