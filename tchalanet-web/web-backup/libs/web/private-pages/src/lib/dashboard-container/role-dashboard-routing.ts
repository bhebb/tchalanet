import { inject,Injectable } from '@angular/core';
import { CanMatchFn, Route, Router, UrlSegment } from '@angular/router';

import { AuthService } from '@tchl/shared/auth';

// Mapping simple role -> segment de route dashboard
const ROLE_DASHBOARD_ROUTES: Record<string, string> = {
  SUPER_ADMIN: 'super-admin',
  TENANT_ADMIN: 'tenant-admin',
  CASHIER: 'cashier',
  VENDEUR: 'cashier',
};

const DEFAULT_DASHBOARD_SEGMENT = 'tenant-admin';

function resolveDashboardSegmentFromRoles(roles: string[] | undefined | null): string {
  if (!roles || roles.length === 0) {
    return DEFAULT_DASHBOARD_SEGMENT;
  }

  for (const role of roles) {
    const normalized = role?.toUpperCase();
    if (normalized && ROLE_DASHBOARD_ROUTES[normalized]) {
      return ROLE_DASHBOARD_ROUTES[normalized];
    }
  }

  return DEFAULT_DASHBOARD_SEGMENT;
}

@Injectable({ providedIn: 'root' })
export class DashboardRoleResolverService {
  private readonly auth = inject(AuthService);

  getTargetSegment(): string {
    const tch = this.auth.tch();
    const roles = tch?.roles ?? [];
    return resolveDashboardSegmentFromRoles(roles);
  }
}

export const dashboardRoleRedirectCanMatch: CanMatchFn = (route: Route, segments: UrlSegment[]) => {
  const router = inject(Router);
  const svc = inject(DashboardRoleResolverService);

  // On ne matche jamais directement cette route : on redirige vers la bonne URL
  const targetSegment = svc.getTargetSegment();

  void router.navigate(['app', 'dashboard', targetSegment]);

  return false;
};
