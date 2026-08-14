import { inject } from '@angular/core';
import { Route } from '@angular/router';
import { Router } from '@angular/router';
import { ForgotPasswordPage, LoginPage } from '@tch/core/auth';

import { environment } from '../environments/environment';

const publicLoginEnabledGuard = () =>
  environment.publicLoginEnabled ? true : inject(Router).parseUrl('/public');

export const appRoutes: Route[] = [
  {
    path: 'login',
    canActivate: [publicLoginEnabledGuard],
    component: LoginPage,
  },
  {
    path: 'forgot-password',
    canActivate: [publicLoginEnabledGuard],
    component: ForgotPasswordPage,
  },
  {
    path: '',
    loadComponent: () =>
      import('./features/public/shell/public-shell.component').then(m => m.TchPublicShellComponent),
    loadChildren: () => import('./features/public/public.routes').then(m => m.publicRoutes),
  },
];
