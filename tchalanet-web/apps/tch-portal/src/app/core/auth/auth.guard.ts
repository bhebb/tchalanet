import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';

import { UserRole } from '../../shared/types';
import { AuthSessionService } from './auth-session.service';

export const authGuard: CanActivateFn = async (): Promise<boolean | UrlTree> => {
  const auth = inject(AuthSessionService);
  const session = await auth.refreshSession();

  if (session.authenticated) {
    return true;
  }

  return inject(Router).parseUrl('/login');
};

// Post-login entry point: /app resolves the user's space from roles and redirects
// to the matching dashboard. The public header login button targets this route.
export const spaceDispatchGuard: CanActivateFn = async (): Promise<UrlTree> => {
  const auth = inject(AuthSessionService);
  const router = inject(Router);
  const session = await auth.refreshSession();

  if (!session.authenticated) {
    return router.parseUrl('/login');
  }

  if (session.roles.includes('SUPER_ADMIN')) {
    return router.parseUrl('/app/platform');
  }
  if (session.roles.includes('TENANT_ADMIN')) {
    return router.parseUrl('/app/admin');
  }
  if (session.roles.includes('CASHIER')) {
    return router.parseUrl('/app/cashier');
  }

  return router.parseUrl('/forbidden');
};

export function roleGuard(requiredRole: UserRole): CanActivateFn {
  return async (): Promise<boolean | UrlTree> => {
    const auth = inject(AuthSessionService);
    const router = inject(Router);
    const session = await auth.refreshSession();

    if (!session.authenticated) {
      return router.parseUrl('/login');
    }

    return auth.hasRole(requiredRole) ? true : router.parseUrl('/forbidden');
  };
}
