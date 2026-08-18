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
      import('./pages/settings/admin-params-overview.page').then(m => m.AdminParamsOverviewPage),
  },
  {
    path: 'settings/receipt',
    loadComponent: () =>
      import('./pages/settings/admin-receipt-config.page').then(m => m.AdminReceiptConfigPage),
  },
  {
    path: 'settings/delivery',
    loadComponent: () =>
      import('./pages/settings/admin-delivery-config.page').then(m => m.AdminDeliveryConfigPage),
  },
  {
    path: 'settings/calendar',
    loadComponent: () =>
      import('./pages/settings/admin-calendar-config.page').then(m => m.AdminCalendarConfigPage),
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
