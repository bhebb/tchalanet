import { inject } from '@angular/core';
import { Router } from '@angular/router';

import { AuthService } from './services/auth.service';

export function RoleGuard(...roles: string[]) {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);

    // return roles.some(r => auth.hasRole(r)) ? true : router.parseUrl('/forbidden');
  };
}
