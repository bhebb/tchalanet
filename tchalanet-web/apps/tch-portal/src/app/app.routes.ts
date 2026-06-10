import { Route } from '@angular/router';

import { roleGuard } from './core/auth/auth.guard';
import { ForbiddenPage } from './core/auth/forbidden.page';
import { NotFoundPage } from '@tch/web';

export const appRoutes: Route[] = [
  {
    path: 'public',
    loadComponent: () =>
      import('./features/public/shell/public-shell.component').then(
        m => m.TchPublicShellComponent,
      ),
    loadChildren: () =>
      import('./features/public/public.routes').then(m => m.publicRoutes),
  },
  {
    path: 'forbidden',
    component: ForbiddenPage,
  },
  {
    path: 'app/platform',
    loadComponent: () =>
      import('./features/private/shell/private-shell.component').then(
        m => m.PrivateShellComponent,
      ),
    canActivate: [roleGuard('SUPER_ADMIN')],
    loadChildren: () =>
      import('./features/platform/platform.routes').then(m => m.platformRoutes),
  },
  {
    path: 'app/admin',
    loadComponent: () =>
      import('./features/private/shell/private-shell.component').then(
        m => m.PrivateShellComponent,
      ),
    canActivate: [roleGuard('TENANT_ADMIN')],
    loadChildren: () => import('./features/admin/admin.routes').then(m => m.adminRoutes),
  },
  {
    path: 'app/cashier',
    loadComponent: () =>
      import('./features/private/shell/private-shell.component').then(
        m => m.PrivateShellComponent,
      ),
    canActivate: [roleGuard('CASHIER')],
    loadChildren: () =>
      import('./features/cashier/cashier.routes').then(m => m.cashierRoutes),
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
