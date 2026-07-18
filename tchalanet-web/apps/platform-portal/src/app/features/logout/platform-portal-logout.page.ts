import { DOCUMENT } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthSessionService } from '@tch/core/auth';
import { TchRuntimeConfigStore } from '@tch/shared-config';

/**
 * Clears the platform session before returning to a portal that requested a
 * cross-portal logout. The return target is restricted to the configured admin
 * login route so this endpoint cannot become an open redirect.
 */
@Component({
  selector: 'tch-platform-portal-logout-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: '',
})
export class PlatformPortalLogoutPage implements OnInit {
  private readonly auth = inject(AuthSessionService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly runtimeConfig = inject(TchRuntimeConfigStore);
  private readonly document = inject(DOCUMENT);

  ngOnInit(): void {
    void this.logout();
  }

  private async logout(): Promise<void> {
    await this.auth.logout();

    const returnTo = this.route.snapshot.queryParamMap.get('returnTo');
    if (returnTo && this.isConfiguredAdminLogin(returnTo)) {
      this.document.defaultView?.location.assign(returnTo);
      return;
    }

    await this.router.navigateByUrl('/login');
  }

  private isConfiguredAdminLogin(candidate: string): boolean {
    const adminBaseUrl = withoutTrailingSlash(
      this.runtimeConfig.config().portalBaseUrls?.['admin-portal'] ?? '/admin',
    );
    const origin = this.document.defaultView?.location.origin;
    if (!origin) return false;

    try {
      const expected = new URL(`${adminBaseUrl}/login`, origin);
      const actual = new URL(candidate, origin);
      return actual.origin === expected.origin && actual.pathname === expected.pathname && !actual.search && !actual.hash;
    } catch {
      return false;
    }
  }
}

function withoutTrailingSlash(value: string): string {
  return value.length > 1 ? value.replace(/\/+$/, '') : value;
}
