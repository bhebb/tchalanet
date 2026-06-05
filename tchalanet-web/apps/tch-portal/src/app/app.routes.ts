import { Route } from '@angular/router';

import { roleGuard } from './core/auth/auth.guard';
import { TenantAdminDashboardPage } from './features/admin/tenant-admin-dashboard.page';
import { ForbiddenPage } from './features/auth/forbidden.page';
import { RoleDashboardPage } from './features/dashboard/role-dashboard.page';
import { SuperAdminDashboardPage } from './features/platform/super-admin-dashboard.page';
import { PublicHomePage } from './features/public/public-home.page';
import { PublicInfoPage } from './features/public/public-info.page';
import { PrivateShellPage } from './features/shell/private-shell.page';

export const appRoutes: Route[] = [
  {
    path: 'public',
    component: PublicHomePage,
  },
  {
    path: 'public/check-ticket',
    component: PublicInfoPage,
    data: { kind: 'check-ticket' },
  },
  {
    path: 'public/results',
    component: PublicInfoPage,
    data: { kind: 'results' },
  },
  {
    path: 'public/results/:id',
    component: PublicInfoPage,
    data: { kind: 'results' },
  },
  {
    path: 'public/rules',
    component: PublicInfoPage,
    data: { kind: 'rules' },
  },
  {
    path: 'public/help',
    component: PublicInfoPage,
    data: { kind: 'help' },
  },
  {
    path: 'public/contact',
    component: PublicInfoPage,
    data: { kind: 'contact' },
  },
  {
    path: 'public/privacy',
    component: PublicInfoPage,
    data: { kind: 'privacy' },
  },
  {
    path: 'public/terms',
    component: PublicInfoPage,
    data: { kind: 'terms' },
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
        component: TenantAdminDashboardPage,
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
        component: SuperAdminDashboardPage,
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
