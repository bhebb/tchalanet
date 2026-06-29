import { DOCUMENT, isPlatformBrowser } from '@angular/common';
import { Injectable, PLATFORM_ID, inject } from '@angular/core';
import { Router } from '@angular/router';
import { TchAppId, TchRuntimeConfigStore } from '@tch/shared-config';

import { UserSession } from './auth.types';

type PortalAppId = Exclude<TchAppId, 'public-portal'>;

const DEFAULT_PORTAL_BASE_URLS: Record<PortalAppId, string> = {
  'admin-portal': '/admin',
  'platform-portal': '/platform',
};

@Injectable({ providedIn: 'root' })
export class AuthRedirectService {
  private readonly document = inject(DOCUMENT);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly router = inject(Router);
  private readonly runtimeConfig = inject(TchRuntimeConfigStore);

  async navigateAfterLogin(session: UserSession): Promise<void> {
    const route = this.postLoginRoute(session);
    const targetApp = this.targetApp(session, route);
    const currentApp = this.runtimeConfig.config().appId;

    if (currentApp === targetApp) {
      await this.router.navigateByUrl(route);
      return;
    }

    if (currentApp === 'public-portal' && targetApp) {
      this.assignBrowserUrl(`${this.portalBaseUrl(targetApp)}${route}`);
      return;
    }

    await this.router.navigateByUrl('/forbidden');
  }

  postLoginRoute(session: UserSession): string {
    if (session.entryRoute) {
      return session.entryRoute;
    }

    if (session.roles.includes('SUPER_ADMIN')) {
      return '/app/platform';
    }
    if (session.roles.includes('TENANT_ADMIN')) {
      return '/app/admin';
    }

    return '/forbidden';
  }

  private targetApp(session: UserSession, route: string): PortalAppId | null {
    if (route.startsWith('/app/platform')) {
      return 'platform-portal';
    }
    if (
      route.startsWith('/app/admin') ||
      route.startsWith('/app/account') ||
      route.startsWith('/app/profile') ||
      route.startsWith('/app/seller-terminal')
    ) {
      return 'admin-portal';
    }

    if (session.roles.includes('SUPER_ADMIN')) {
      return 'platform-portal';
    }
    if (session.roles.includes('TENANT_ADMIN')) {
      return 'admin-portal';
    }

    return null;
  }

  private portalBaseUrl(appId: PortalAppId): string {
    return withoutTrailingSlash(
      this.runtimeConfig.config().portalBaseUrls?.[appId] ?? DEFAULT_PORTAL_BASE_URLS[appId],
    );
  }

  private assignBrowserUrl(url: string): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    this.document.defaultView?.location.assign(url);
  }
}

function withoutTrailingSlash(value: string): string {
  return value.length > 1 ? value.replace(/\/+$/, '') : value;
}
