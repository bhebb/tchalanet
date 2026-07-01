import { Route } from '@angular/router';

export const adminLimitsRoutes: Route[] = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/shell/admin-limits-shell.page').then(m => m.AdminLimitsShellPage),
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./pages/overview/admin-limits-overview.page').then(m => m.AdminLimitsOverviewPage),
      },
      {
        path: 'system',
        loadComponent: () =>
          import('./pages/system/admin-limits-system.page').then(m => m.AdminLimitsSystemPage),
      },
      {
        path: 'global',
        loadComponent: () =>
          import('./pages/global/admin-limits-global.page').then(m => m.AdminLimitsGlobalPage),
      },
      {
        path: 'draw',
        loadComponent: () =>
          import('./pages/draw/admin-limits-draw.page').then(m => m.AdminLimitsDrawPage),
      },
      {
        path: 'seller-terminal',
        loadComponent: () =>
          import('./pages/seller-terminal/admin-limits-seller-terminal.page').then(m => m.AdminLimitsSellerTerminalPage),
      },
      {
        path: 'number',
        loadComponent: () =>
          import('./pages/number/admin-limits-number.page').then(m => m.AdminLimitsNumberPage),
      },
      // Backwards compat: old /limits/rules redirects to global tab
      { path: 'rules', redirectTo: 'global', pathMatch: 'full' },
    ],
  },
];
