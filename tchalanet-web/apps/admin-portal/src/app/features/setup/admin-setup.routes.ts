import { Route } from '@angular/router';

export const adminSetupRoutes: Route[] = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/complete-config/admin-complete-tenant-config.page').then(
        m => m.AdminCompleteTenantConfigPage,
      ),
  },
  {
    path: 'settings',
    loadComponent: () =>
      import('./pages/settings/admin-settings.page').then(m => m.AdminSettingsPage),
  },
  {
    path: 'settings/config',
    loadComponent: () =>
      import('./pages/settings/admin-config.page').then(m => m.AdminConfigPage),
  },
  {
    path: 'settings/runtime',
    loadComponent: () =>
      import('./pages/settings/admin-runtime.page').then(m => m.AdminRuntimePage),
  },
];
