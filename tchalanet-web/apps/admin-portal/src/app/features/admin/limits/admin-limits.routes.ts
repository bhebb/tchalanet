import { Route } from '@angular/router';

export const adminLimitsRoutes: Route[] = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/shell/admin-limits-shell.page').then(m => m.AdminLimitsShellPage),
    children: [
      { path: '', redirectTo: 'global', pathMatch: 'full' },
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
        path: 'agent',
        loadComponent: () =>
          import('./pages/agent/admin-limits-agent.page').then(m => m.AdminLimitsAgentPage),
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
