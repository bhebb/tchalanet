import { Route } from '@angular/router';

import { roleGuard } from './core/auth/auth.guard';
import { TenantAdminDashboardPage } from './features/admin/tenant-admin-dashboard.page';
import { ForbiddenPage } from './core/auth/forbidden.page';
import { RoleDashboardPage } from './features/dashboard/role-dashboard.page';
import { SuperAdminDashboardPage } from './features/platform/super-admin-dashboard.page';
import { PublicCheckTicketPage } from './features/public/public-check-ticket.page';
import { PublicHomePage } from './features/public/public-home.page';
import { PublicInfoPage } from './features/public/public-info.page';
import { PublicRulesPage } from './features/public/public-rules.page';
import { PublicResultDetailPage } from './features/public/public-result-detail.page';
import { PublicResultsPage } from './features/public/public-results.page';
import { PrivateShellPage } from './features/dashboard/shell/private-shell.page';

export const appRoutes: Route[] = [
  {
    path: 'public',
    component: PublicHomePage,
  },
  {
    path: 'public/check-ticket',
    component: PublicCheckTicketPage,
  },
  {
    path: 'public/results',
    component: PublicResultsPage,
  },
  {
    path: 'public/results/:id',
    component: PublicResultDetailPage,
  },
  {
    path: 'public/rules',
    component: PublicRulesPage,
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
