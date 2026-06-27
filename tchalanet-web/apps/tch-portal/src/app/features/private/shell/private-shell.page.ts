import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { exhaustMap, filter, from, interval } from 'rxjs';
import { ActionItem, NavigationDestination } from '@tch/api';
import { TchBrand, TchSidebarNav, TchUserMenu } from '@tch/ui/components';
import { ThemeStore } from '@tch/ui/theme';

import { AuthSessionService } from '../../../core/auth/auth-session.service';
import { I18nFacade, LanguageSwitcher } from '../../../core/i18n';
import { AppRuntimeStore } from '../../../core/runtime';
import { ShellFeedbackOutletComponent } from '../../../core/feedback/shell-feedback-outlet.component';
import { ShellFeedbackStore } from '../../../core/feedback/shell-feedback.store';
import { ShellFeedbackVerbosity } from '../../../core/feedback/shell-feedback.model';
import { PrivateNotificationBellComponent } from '../../../core/notifications/private-notification-bell.component';
import { PrivateNotificationsStore } from '../../../core/notifications/private-notifications.store';
import { PrivateShellService } from './private-shell.service';
import { AdminOverrideBanner } from '../shared/admin-override-banner';
import { environment } from '../../../../environments/environment';

const DEFAULT_NOTIFICATIONS_POLL_MS = 20 * 60 * 1000;
const DEFAULT_SESSION_POLL_MS = 30 * 60 * 1000;

@Component({
  imports: [
    AdminOverrideBanner,
    LanguageSwitcher,
    PrivateNotificationBellComponent,
    RouterOutlet,
    ShellFeedbackOutletComponent,
    TchBrand,
    TchSidebarNav,
    TchUserMenu,
    TranslatePipe,
  ],
  selector: 'tch-private-shell-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '(document:keydown.escape)': 'closeDrawer()' },
  template: `
    <div class="private-shell">
      <header class="top-app-bar">
        <button
          type="button"
          class="burger"
          (click)="toggleDrawer()"
          [attr.aria-label]="'nav.toggle_menu' | translate"
          [attr.aria-expanded]="drawerOpen()"
          aria-controls="private-drawer"
        >
          <span class="material-symbols-outlined" aria-hidden="true">menu</span>
        </button>
        <tch-brand [brand]="brand()" [showName]="false" />
        @if (titleKey()) {
          <strong>{{ titleKey() | translate }}</strong>
        }
        <div class="utilities">
          <tch-private-notification-bell />
          <tch-language-switcher />
          <button
            type="button"
            class="mode-toggle"
            (click)="toggleMode()"
            [attr.aria-label]="'nav.theme_toggle' | translate"
            [attr.aria-pressed]="isDark()"
          >
            <span class="material-symbols-outlined" aria-hidden="true">
              {{ isDark() ? 'light_mode' : 'dark_mode' }}
            </span>
          </button>
          <tch-user-menu
            [name]="userName()"
            (profile)="openProfile()"
            (settings)="comingSoon()"
            (logout)="logout()"
          />
        </div>
      </header>

      <div class="workspace" [class.is-drawer-open]="drawerOpen()">
        <aside id="private-drawer" class="drawer">
          <tch-sidebar-nav
            [primary]="primary()"
            [sections]="shellSvc.navigation()"
            [secondary]="shell()?.navigationDrawer?.secondary ?? []"
          />
        </aside>

        <div class="backdrop" (click)="closeDrawer()" aria-hidden="true"></div>

        <main class="content">
          <tch-admin-override-banner />
          <tch-shell-feedback-outlet [verbosity]="feedbackVerbosity()" />
          <router-outlet />
        </main>
      </div>
    </div>
  `,
  styles: [
    `
      .private-shell {
        --comp-private-border: var(--tch-color-outline-variant);
        min-height: 100vh;
      }

      .top-app-bar {
        position: sticky;
        top: 0;
        z-index: var(--tch-z-header, 100);
        align-items: center;
        border-bottom: 1px solid var(--comp-private-border);
        background: var(--tch-color-surface, #fff);
        display: flex;
        justify-content: space-between;
        min-height: 4rem;
        padding: 0 1.5rem;
      }

      .utilities {
        align-items: center;
        display: flex;
        flex-wrap: wrap;
        gap: 0.75rem;
        justify-content: flex-end;
      }

      .mode-toggle {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 2.5rem;
        height: 2.5rem;
        border: 0;
        border-radius: 9999px;
        background: transparent;
        color: inherit;
        cursor: pointer;
      }

      .mode-toggle:hover {
        background: var(--tch-color-surface-container-high);
      }

      .workspace {
        display: grid;
        grid-template-columns: minmax(12rem, 16rem) 1fr;
        min-height: calc(100vh - 4rem);
      }

      .drawer {
        min-width: 0;
      }

      .content {
        min-width: 0;
      }

      /* Burger + backdrop are desktop-hidden; the sidebar is a static grid column. */
      .burger {
        display: none;
        align-items: center;
        justify-content: center;
        width: 2.75rem;
        height: 2.75rem;
        border: 0;
        border-radius: var(--tch-radius-md, 12px);
        background: transparent;
        color: inherit;
        cursor: pointer;
      }

      .burger:hover {
        background: var(--tch-color-surface-container-high);
      }

      .backdrop {
        display: none;
      }

      @media (max-width: 720px) {
        .top-app-bar {
          flex-wrap: wrap;
          gap: 0.5rem 0.75rem;
          padding-block: 0.75rem;
        }

        .burger {
          display: inline-flex;
        }

        .workspace {
          grid-template-columns: 1fr;
        }

        /* Off-canvas drawer */
        .drawer {
          position: fixed;
          inset: 0 auto 0 0;
          width: min(82vw, 18rem);
          transform: translateX(-100%);
          transition: transform 0.2s ease;
          z-index: var(--tch-z-drawer, 300);
          overflow-y: auto;
          box-shadow: var(--tch-elevation-2, 0 8px 24px rgba(0, 0, 0, 0.18));
        }

        .workspace.is-drawer-open .drawer {
          transform: translateX(0);
        }

        .workspace.is-drawer-open .backdrop {
          display: block;
          position: fixed;
          inset: 0;
          z-index: calc(var(--tch-z-drawer, 300) - 10);
          background: color-mix(in oklab, #000 45%, transparent);
        }
      }

      @media (prefers-reduced-motion: reduce) {
        .drawer {
          transition: none;
        }
      }
    `,
  ],
})
export class PrivateShellPage {
  private readonly auth = inject(AuthSessionService);
  private readonly runtime = inject(AppRuntimeStore);
  private readonly notifications = inject(PrivateNotificationsStore);
  private readonly router = inject(Router);
  private readonly theme = inject(ThemeStore);
  private readonly feedback = inject(ShellFeedbackStore);
  private readonly i18n = inject(I18nFacade);
  protected readonly shellSvc = inject(PrivateShellService);

  readonly shell = this.shellSvc.shell;

  /** Mobile off-canvas drawer state. Desktop ignores it (sidebar is a static column). */
  readonly drawerOpen = signal(false);

  /** Light/dark icon toggle (personal display preference — available to every role). */
  readonly isDark = computed(() => this.theme.activeTheme().effectiveMode === 'dark');

  readonly userName = computed(
    () => this.auth.session().displayName || this.auth.session().username || '',
  );

  /**
   * Normalize the runtime brand for `TchBrand`: the payload may carry `image` as a string or as an
   * object (`{ url }`), and a `path` instead of a `destination`. Tolerant until the BFF resolves it.
   */
  readonly brand = computed<ActionItem>(() => {
    const raw = this.shell()?.navigationDrawer?.brand as BrandLike | undefined;
    const resolved = typeof raw?.image === 'string' ? raw.image : raw?.image?.url;
    const image = resolved ?? DEFAULT_LOGO;
    const destination: NavigationDestination =
      raw?.destination ??
      (raw?.path ? { kind: 'route', value: raw.path } : roleHome(this.auth.session().roles));
    return { id: raw?.id ?? 'brand', labelKey: raw?.labelKey, label: raw?.label ?? 'Tchalanet', image, destination };
  });

  readonly feedbackVerbosity = computed<ShellFeedbackVerbosity>(() =>
    this.auth.session().roles.includes('SUPER_ADMIN') ? 'verbose' : 'standard',
  );

  readonly titleKey = computed(() => this.shell()?.topAppBar?.titleKey ?? '');

  readonly primary = computed(() => {
    const resolved = this.shell()?.navigationDrawer?.primary ?? [];
    const sections = this.shell()?.navigationDrawer?.sections ?? [];
    if (this.shell() && sections.length) return resolved;
    // sections empty → sections-based fallback handles nav exclusively, primary must be []
    return [];
  });

  constructor() {
    this.runtime.initPrivateRuntime();
    this.notifications.loadLatest();
    this.startShellPolling();

    effect(() => {
      if (this.shellSvc.shellLoadError()) {
        this.feedback.add({
          severity: 'warn',
          title: this.i18n.instant('shell.error.backendUnavailable.title'),
          message: this.i18n.instant('shell.error.backendUnavailable.message'),
        });
      }
    });

    // Close the mobile drawer after any navigation (tapping a nav link).
    this.router.events
      .pipe(
        filter((e): e is NavigationEnd => e instanceof NavigationEnd),
        takeUntilDestroyed(),
      )
      .subscribe(() => this.drawerOpen.set(false));
  }

  private startShellPolling(): void {
    const polling = environment.privateShellPolling ?? {};
    const notificationsPollMs = positiveInterval(
      polling.notificationsMs,
      DEFAULT_NOTIFICATIONS_POLL_MS,
    );
    const sessionPollMs = positiveInterval(polling.sessionMs, DEFAULT_SESSION_POLL_MS);

    interval(notificationsPollMs)
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.notifications.loadLatest({ silent: true }));

    interval(sessionPollMs)
      .pipe(
        exhaustMap(() => from(this.auth.refreshSession(true))),
        takeUntilDestroyed(),
      )
      .subscribe({
        next: session => {
          if (!session.authenticated) {
            void this.router.navigate(['/login']);
          }
        },
      });
  }

  toggleDrawer(): void {
    this.drawerOpen.update(open => !open);
  }

  closeDrawer(): void {
    this.drawerOpen.set(false);
  }

  toggleMode(): void {
    this.theme.setMode(this.isDark() ? 'light' : 'dark');
  }

  /** Profil / Paramètres pages are not built yet — surface a non-blocking notice for now. */
  comingSoon(): void {
    this.feedback.add({
      severity: 'info',
      title: this.i18n.instant('common.coming_soon'),
      message: '',
    });
  }

  openProfile(): void {
    void this.router.navigate(['/app/profile']);
  }

  logout(): void {
    void this.auth.logout().then(() => this.router.navigate(['/login']));
  }
}

function positiveInterval(value: number | undefined, fallback: number): number {
  return typeof value === 'number' && Number.isFinite(value) && value > 0 ? value : fallback;
}

const DEFAULT_LOGO = '/assets/brand/tchalanet-logo.svg';

interface BrandLike {
  readonly id?: string;
  readonly labelKey?: string;
  readonly label?: string | null;
  readonly image?: string | { url?: string } | null;
  readonly path?: string;
  readonly destination?: NavigationDestination;
}

function roleHome(roles: readonly string[]): NavigationDestination {
  const value = roles.includes('SUPER_ADMIN')
    ? '/app/platform'
    : roles.includes('TENANT_ADMIN')
      ? '/app/admin'
      : '/app/cashier';
  return { kind: 'route', value };
}

function fallbackDestinations(roles: readonly string[]): readonly ActionItem[] {
  const route = roles.includes('SUPER_ADMIN')
    ? '/app/platform'
    : roles.includes('TENANT_ADMIN')
      ? '/app/admin'
      : '/app/cashier';
  const labelKey = roles.includes('SUPER_ADMIN')
    ? 'dashboard.titles.platform'
    : roles.includes('TENANT_ADMIN')
      ? 'dashboard.titles.admin'
      : 'dashboard.titles.cashier';
  return [{ id: 'dashboard', labelKey, destination: { kind: 'route', value: route } }];
}
