import { Route } from '@angular/router';

import { roleGuard } from './core/auth/auth.guard';
import { ForbiddenPage } from './features/auth/forbidden.page';
import { RoleDashboardPage } from './features/dashboard/role-dashboard.page';
import { PublicHomePage } from './features/public/public-home.page';
import { PrivateShellPage } from './features/shell/private-shell.page';

export const appRoutes: Route[] = [
  {
    path: 'public',
    component: PublicHomePage,
  },
  {
    path: 'forbidden',
    component: ForbiddenPage,
  },
  {
    path: 'app/cashier',
    component: PrivateShellPage,
    canActivate: [roleGuard('CASHIER')],
    children: [
      {
        path: '',
        component: RoleDashboardPage,
        data: {
          titleKey: 'dashboard.titles.cashier',
        },
      },
    ],
  },
  {
    path: 'app/admin',
    component: PrivateShellPage,
    canActivate: [roleGuard('TENANT_ADMIN')],
    children: [
      {
        path: '',
        component: RoleDashboardPage,
        data: {
          titleKey: 'dashboard.titles.admin',
        },
      },
    ],
  },
  {
    path: 'app/platform',
    component: PrivateShellPage,
    canActivate: [roleGuard('SUPER_ADMIN')],
    children: [
      {
        path: '',
        component: RoleDashboardPage,
        data: {
          titleKey: 'dashboard.titles.platform',
        },
      },
    ],
  },
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'public',
  },
  {
    path: '**',
    redirectTo: 'public',
  },
];
