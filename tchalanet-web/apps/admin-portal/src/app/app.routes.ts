import { Route } from '@angular/router';
import {
  ForbiddenPage,
  ForgotPasswordPage,
  LoginPage,
  authGuard,
  roleGuard,
  spaceDispatchGuard,
} from '@tch/core/auth';
import { consoleAccountRoutes, consoleProfileRoutes } from '@tch/ui/console';

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
    path: 'account/activation',
    canActivate: [roleGuard('TENANT_ADMIN')],
    children: consoleAccountRoutes,
  },
  {
    path: 'profile',
    canActivate: [authGuard],
    children: consoleProfileRoutes,
  },
  {
    path: 'seller-terminal/activation',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/admin/seller-terminals/pages/activation/seller-terminal-activation.page').then(
        m => m.SellerTerminalActivationPage,
      ),
  },
  {
    path: 'pos',
    redirectTo: 'app/admin/pos/sale',
    pathMatch: 'full',
  },
  {
    path: 'app',
    pathMatch: 'full',
    canActivate: [spaceDispatchGuard],
    children: [],
  },
  {
    path: 'app/account/activation',
    redirectTo: 'account/activation',
    pathMatch: 'full',
  },
  {
    path: 'app/profile',
    redirectTo: 'profile',
    pathMatch: 'full',
  },
  {
    path: 'app/seller-terminal/activation',
    redirectTo: 'seller-terminal/activation',
    pathMatch: 'full',
  },
  {
    path: 'app/admin',
    canActivate: [roleGuard('TENANT_ADMIN')],
    loadChildren: () => import('./features/admin/admin.routes').then(m => m.adminRoutes),
  },
  {
    path: 'forbidden',
    component: ForbiddenPage,
  },
  {
    path: '',
    canActivate: [roleGuard('TENANT_ADMIN')],
    loadChildren: () => import('./features/admin/admin.routes').then(m => m.adminRoutes),
  },
  {
    path: '**',
    redirectTo: '',
  },
];
