import { Route } from '@angular/router';

import { roleGuard } from './core/auth/auth.guard';
import { ForbiddenPage } from './features/auth/forbidden.page';
import { RoleDashboardPage } from './features/dashboard/role-dashboard.page';
import { PublicHomePage } from './features/public/public-home.page';

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
    component: RoleDashboardPage,
    canActivate: [roleGuard('CASHIER')],
    data: {
      titleKey: 'dashboard.titles.cashier',
    },
  },
  {
    path: 'app/admin',
    component: RoleDashboardPage,
    canActivate: [roleGuard('TENANT_ADMIN')],
    data: {
      titleKey: 'dashboard.titles.admin',
    },
  },
  {
    path: 'app/platform',
    component: RoleDashboardPage,
    canActivate: [roleGuard('SUPER_ADMIN')],
    data: {
      titleKey: 'dashboard.titles.platform',
    },
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
