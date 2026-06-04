import { appRoutes } from './app.routes';
import { RoleDashboardPage } from './features/dashboard/role-dashboard.page';
import { PrivateShellPage } from './features/shell/private-shell.page';

describe('appRoutes', () => {
  it.each([
    ['app/cashier', 'dashboard.titles.cashier'],
    ['app/admin', 'dashboard.titles.admin'],
    ['app/platform', 'dashboard.titles.platform'],
  ])('guards %s at the private shell route and renders the dashboard child', (path, titleKey) => {
    const route = appRoutes.find((candidate) => candidate.path === path);

    expect(route?.component).toBe(PrivateShellPage);
    expect(route?.canActivate).toHaveLength(1);
    expect(route?.children).toEqual([
      {
        path: '',
        component: RoleDashboardPage,
        data: {
          titleKey,
        },
      },
    ]);
  });
});
