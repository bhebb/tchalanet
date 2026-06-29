import { Route } from '@angular/router';
import { ForgotPasswordPage, LoginPage, authGuard, roleGuard, spaceDispatchGuard } from '@tch/core/auth';
import { consoleProfileRoutes } from '@tch/ui/console';

export const appRoutes: Route[] = [
  {
    path: 'login',
    component: LoginPage,
  },
  {
    path: 'forgot-password',
    component: ForgotPasswordPage,
  },
  {
    path: 'profile',
    canActivate: [authGuard],
    children: consoleProfileRoutes,
  },
  {
    path: 'app',
    pathMatch: 'full',
    canActivate: [spaceDispatchGuard],
    children: [],
  },
  {
    path: 'app/profile',
    redirectTo: 'profile',
    pathMatch: 'full',
  },
  {
    path: 'app/platform',
    canActivate: [roleGuard('SUPER_ADMIN')],
    loadChildren: () => import('./features/platform/platform.routes').then(m => m.platformRoutes),
  },
  {
    path: '',
    canActivate: [roleGuard('SUPER_ADMIN')],
    loadChildren: () => import('./features/platform/platform.routes').then(m => m.platformRoutes),
  },
  {
    path: '**',
    redirectTo: '',
  },
];
