import { Route } from '@angular/router';
import { ForgotPasswordPage, TchLoginPage, authGuard, spaceDispatchGuard } from '@tch/core/auth';
import { consoleProfileRoutes } from '@tch/ui/console';

export const appRoutes: Route[] = [
  {
    path: 'login',
    component: TchLoginPage,
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
    loadChildren: () => import('./features/platform/platform.routes').then(m => m.platformRoutes),
  },
  {
    path: '',
    loadChildren: () => import('./features/platform/platform.routes').then(m => m.platformRoutes),
  },
  {
    path: '**',
    redirectTo: '',
  },
];
