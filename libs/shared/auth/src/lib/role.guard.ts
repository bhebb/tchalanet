import { inject } from '@angular/core';
import { AuthService } from './services/auth.service';
import { Router } from '@angular/router';

export function RoleGuard(...roles: string[]) {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);

    // return roles.some(r => auth.hasRole(r)) ? true : router.parseUrl('/forbidden');
  };
}
