import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { exhaustMap, filter, from, interval } from 'rxjs';
import { ActionItem, NavigationDestination } from '@tch/api';
import { AuthSessionService } from '@tch/core/auth';
import { TCH_BRAND_ASSETS } from '@tch/shared-assets';
import { ThemeStore } from '@tch/ui/theme';
import {
  PrivateShellLayoutComponent,
  ShellFeedbackStore,
  ShellFeedbackVerbosity,
} from '@tch/web/shell';

import { I18nFacade, LanguageSwitcher } from '@tch/core/i18n';
import { AppRuntimeStore } from '../../../core/runtime';
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
    PrivateShellLayoutComponent,
    RouterOutlet,
  ],
  selector: 'tch-private-shell-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '(document:keydown.escape)': 'closeDrawer()' },
  templateUrl: './private-shell.page.html',
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
    return {
      id: raw?.id ?? 'brand',
      labelKey: raw?.labelKey,
      label: raw?.label ?? 'Tchalanet',
      image,
      destination,
    };
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

const DEFAULT_LOGO = TCH_BRAND_ASSETS.logo;

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
