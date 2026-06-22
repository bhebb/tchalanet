import { Route } from '@angular/router';

import { authGuard, roleGuard, spaceDispatchGuard } from './core/auth/auth.guard';
import { AccessStatePage } from './core/auth/access-state.page';
import { ForbiddenPage } from './core/auth/forbidden.page';
import { NotFoundPage } from '@tch/web';

export const appRoutes: Route[] = [
  {
    path: 'login',
    loadComponent: () => import('./core/auth/firebase/login.page').then(m => m.LoginPage),
  },
  {
    path: 'public',
    loadComponent: () =>
      import('./features/public/shell/public-shell.component').then(m => m.TchPublicShellComponent),
    loadChildren: () => import('./features/public/public.routes').then(m => m.publicRoutes),
  },
  {
    path: 'forbidden',
    component: ForbiddenPage,
  },
  {
    path: 'app/access-denied',
    component: ForbiddenPage,
  },
  {
    path: 'app/access-not-configured',
    canActivate: [authGuard],
    component: AccessStatePage,
    data: {
      title: 'Accès non configuré',
      body: "Votre compte existe, mais aucun accès plateforme ou tenant n'est encore configuré.",
    },
  },
  {
    path: 'app/select-tenant',
    canActivate: [authGuard],
    component: AccessStatePage,
    data: {
      title: 'Sélection tenant',
      body: 'Plusieurs contextes tenant sont disponibles. La sélection dédiée sera branchée ici.',
    },
  },
  {
    // Post-login dispatcher: always redirects to the role-appropriate space.
    path: 'app',
    pathMatch: 'full',
    canActivate: [spaceDispatchGuard],
    children: [],
  },
  {
    path: 'app/account/activation',
    canActivate: [roleGuard('TENANT_ADMIN')],
    loadComponent: () =>
      import('./features/private/account/account-activation.page').then(m => m.AccountActivationPage),
  },
  {
    path: 'app/seller-terminal/activation',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/private/seller-terminal/seller-terminal-activation.page').then(
        m => m.SellerTerminalActivationPage,
      ),
  },
  {
    path: 'app/platform',
    loadComponent: () =>
      import('./features/private/shell/private-shell.page').then(m => m.PrivateShellPage),
    canActivate: [roleGuard('SUPER_ADMIN')],
    loadChildren: () => import('./features/private/platform/platform.routes').then(m => m.platformRoutes),
  },
  {
    path: 'app/admin',
    loadComponent: () =>
      import('./features/private/shell/private-shell.page').then(m => m.PrivateShellPage),
    canActivate: [roleGuard('TENANT_ADMIN')],
    loadChildren: () => import('./features/private/admin/admin.routes').then(m => m.adminRoutes),
  },
  {
    path: 'app/cashier',
    loadComponent: () =>
      import('./features/private/shell/private-shell.page').then(m => m.PrivateShellPage),
    canActivate: [roleGuard('CASHIER')],
    loadChildren: () => import('./features/private/seller-terminal/seller-terminal.routes').then(m => m.sellerTerminalRoutes),
  },
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'public',
  },
  {
    path: '**',
    component: NotFoundPage,
  },
];
