import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { ActionItem } from '@tch/api';
import {
  AuthSessionService,
  PrivateBootstrapStore,
  SupportAccessStore,
  sectionsFromRuntimeNavigation,
} from '@tch/core/auth';
import { TchRuntimeConfigStore } from '@tch/shared-config';
import { ThemeStore } from '@tch/ui/theme';
import { ThemeSandboxComponent } from '@tch/web/sandbox';
import { PrivateShellLayoutComponent, TENANT_ADMIN_NAVIGATION } from '@tch/web/shell';
import { filter, map, startWith } from 'rxjs';

const ADMIN_BRAND: ActionItem = {
  id: 'admin-brand',
  labelKey: 'app.name',
  image: '/assets/brand/tchalanet-logo.svg',
  destination: { kind: 'route', value: '/app/admin' },
};

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [PrivateShellLayoutComponent, RouterOutlet, ThemeSandboxComponent],
  selector: 'tch-root',
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  private readonly router = inject(Router);
  private readonly auth = inject(AuthSessionService);
  private readonly bootstrap = inject(PrivateBootstrapStore);
  private readonly runtimeConfig = inject(TchRuntimeConfigStore);
  private readonly supportAccess = inject(SupportAccessStore);
  private readonly theme = inject(ThemeStore);

  protected readonly brand = ADMIN_BRAND;
  protected readonly primary = computed<readonly ActionItem[]>(() => {
    const session = this.supportAccess.session();
    if (!session) {
      return [];
    }

    const platformBaseUrl = withoutTrailingSlash(
      this.runtimeConfig.config().portalBaseUrls?.['platform-portal'] ?? '/platform',
    );

    return [
      {
        id: 'support-access-active',
        label: `Support: ${session.tenantName}`,
        icon: session.mode === 'SUPPORT_READONLY' ? 'visibility' : 'support_agent',
        destination: { kind: 'route', value: '/app/admin' },
        disabled: true,
        badge: {
          kind: 'status',
          value: session.mode === 'SUPPORT_READONLY' ? 'RO' : 'ON',
          severity: session.mode === 'SUPPORT_READONLY' ? 'info' : 'warning',
        },
      },
      {
        id: 'support-access-platform',
        label: 'Retour plateforme',
        icon: 'arrow_back',
        destination: { kind: 'url', value: `${platformBaseUrl}/app/platform` },
      },
    ];
  });
  protected readonly sections = computed(() => {
    if (this.bootstrap.space() !== 'ADMIN') {
      return TENANT_ADMIN_NAVIGATION;
    }
    return sectionsFromRuntimeNavigation(this.bootstrap.navigationDrawer()) ?? TENANT_ADMIN_NAVIGATION;
  });
  protected readonly titleKey = 'surface.tenant_admin';
  protected readonly currentUrl = toSignal(
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      map(event => event.urlAfterRedirects),
      startWith(this.router.url),
    ),
    { initialValue: this.router.url },
  );
  protected readonly showShell = computed(() => isPrivateShellRoute(this.currentUrl()));
  protected readonly userName = computed(() => this.auth.session().displayName ?? '');
  protected readonly darkMode = computed(() => this.theme.activeTheme().effectiveMode === 'dark');

  protected toggleTheme(): void {
    this.theme.setMode(this.darkMode() ? 'light' : 'dark');
  }

  protected async logout(): Promise<void> {
    await this.auth.logout();
    await this.router.navigateByUrl('/login');
  }

  protected goToProfile(): void {
    void this.router.navigateByUrl('/profile');
  }
}

function isPrivateShellRoute(url: string): boolean {
  const path = url.split('?')[0] ?? '';
  return !['/login', '/forgot-password', '/forbidden'].some(publicPath =>
    path.startsWith(publicPath),
  );
}

function withoutTrailingSlash(value: string): string {
  return value.length > 1 ? value.replace(/\/+$/, '') : value;
}
