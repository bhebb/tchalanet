import { Route } from '@angular/router';
import {
  ForbiddenPage,
  ForgotPasswordPage,
  LoginPage,
  PortalHandoffPage,
  authGuard,
  roleGuard,
  spaceDispatchGuard,
} from '@tch/core/auth';
import { consoleAccountRoutes, consoleProfileRoutes } from '@tch/ui/console';
import { PlatformPortalLogoutPage } from './features/logout/platform-portal-logout.page';

export const appRoutes: Route[] = [
  {
    path: 'logout',
    component: PlatformPortalLogoutPage,
  },
  {
    path: 'login/handoff',
    component: PortalHandoffPage,
  },
  {
    path: 'login',
    component: LoginPage,
  },
  {
    path: 'forgot-password',
    component: ForgotPasswordPage,
  },
  {
    path: 'account/activation',
    canActivate: [authGuard],
    children: consoleAccountRoutes,
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
    path: 'app/account/activation',
    redirectTo: 'account/activation',
    pathMatch: 'full',
  },
  {
    path: 'app/platform',
    canActivate: [roleGuard('SUPER_ADMIN')],
    loadChildren: () => import('./features/platform.routes').then(m => m.platformRoutes),
  },
  {
    path: 'forbidden',
    component: ForbiddenPage,
  },
  {
    path: '',
    canActivate: [roleGuard('SUPER_ADMIN')],
    loadChildren: () => import('./features/platform.routes').then(m => m.platformRoutes),
  },
  {
    path: '**',
    redirectTo: '',
  },
];
