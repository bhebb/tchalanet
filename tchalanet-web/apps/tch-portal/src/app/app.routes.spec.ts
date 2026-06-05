import { Type } from '@angular/core';

import { appRoutes } from './app.routes';
import { TenantAdminDashboardPage } from './features/admin/tenant-admin-dashboard.page';
import { RoleDashboardPage } from './features/dashboard/role-dashboard.page';
import { SuperAdminDashboardPage } from './features/platform/super-admin-dashboard.page';
import { PrivateShellPage } from './features/dashboard/shell/private-shell.page';

describe('appRoutes', () => {
  it.each<[string, string, Type<unknown>]>([
    ['app/cashier', 'dashboard.titles.cashier', RoleDashboardPage],
    ['app/admin', 'dashboard.titles.admin', TenantAdminDashboardPage],
    ['app/platform', 'dashboard.titles.platform', SuperAdminDashboardPage],
  ])(
    'guards %s at the private shell route and renders its dashboard child',
    (path, titleKey, child) => {
      const route = appRoutes.find(candidate => candidate.path === path);

      expect(route?.component).toBe(PrivateShellPage);
      expect(route?.canActivate).toHaveLength(1);
      expect(route?.children).toEqual([
        {
          path: '',
          component: child,
          data: {
            titleKey,
          },
        },
      ]);
    },
  );
});
