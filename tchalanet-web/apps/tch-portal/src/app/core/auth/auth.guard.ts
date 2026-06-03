import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';

import { UserRole } from '../../shared/types';
import { AuthSessionService } from './auth-session.service';

export const authGuard: CanActivateFn = async (): Promise<boolean> => {
  const auth = inject(AuthSessionService);
  const session = await auth.refreshSession();

  if (session.authenticated) {
    return true;
  }

  await auth.login();
  return false;
};

export function roleGuard(requiredRole: UserRole): CanActivateFn {
  return async (): Promise<boolean | UrlTree> => {
    const auth = inject(AuthSessionService);
    const router = inject(Router);
    const session = await auth.refreshSession();

    if (!session.authenticated) {
      await auth.login();
      return false;
    }

    return auth.hasRole(requiredRole) ? true : router.parseUrl('/forbidden');
  };
}
