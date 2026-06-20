import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { filter } from 'rxjs';
import { ActionItem, NavigationDestination } from '@tch/api';
import { TchBrand, TchSidebarNav, TchUserMenu } from '@tch/ui/components';
import { ThemeStore } from '@tch/ui/theme';

import { AuthSessionService } from '../../../core/auth/auth-session.service';
import { I18nFacade, LanguageSwitcher } from '../../../core/i18n';
import { AppRuntimeStore, PrivateBootstrapStore } from '../../../core/runtime';
import { ShellFeedbackOutletComponent } from '../../../shared/feedback/shell-feedback-outlet.component';
import { ShellFeedbackStore } from '../../../shared/feedback/shell-feedback.store';
import { ShellFeedbackVerbosity } from '../../../shared/feedback/shell-feedback.model';
import { PrivateShellService } from '../../private/shell/private-shell.service';
import { AdminOverrideBanner } from '../../private/shared/admin-override-banner';

@Component({
  imports: [
    AdminOverrideBanner,
    LanguageSwitcher,
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
          @if (unreadCount() > 0) {
            <span class="notif-badge" [attr.aria-label]="unreadCount() + ' notifications'">
              {{ unreadCount() }}
            </span>
          }
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
            (profile)="comingSoon()"
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
        align-items: center;
        border-bottom: 1px solid var(--comp-private-border);
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

      .notif-badge {
        align-items: center;
        background: var(--tch-color-error, #ba1a1a);
        border-radius: 9999px;
        color: var(--tch-color-on-error, #fff);
        display: inline-flex;
        font-size: 0.75rem;
        font-weight: 700;
        justify-content: center;
        min-width: 1.5rem;
        padding: 0.125rem 0.375rem;
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
          z-index: 60;
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
          z-index: 55;
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
  private readonly bootstrapStore = inject(PrivateBootstrapStore);
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
    return resolved.length ? resolved : fallbackDestinations(this.auth.session().roles);
  });

  readonly unreadCount = computed(() => this.bootstrapStore.unreadCount());

  constructor() {
    this.runtime.initPrivateRuntime();
    // Close the mobile drawer after any navigation (tapping a nav link).
    this.router.events
      .pipe(
        filter((e): e is NavigationEnd => e instanceof NavigationEnd),
        takeUntilDestroyed(),
      )
      .subscribe(() => this.drawerOpen.set(false));
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

  logout(): void {
    void this.auth.logout().then(() => this.router.navigate(['/login']));
  }
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
