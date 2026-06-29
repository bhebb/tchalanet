import { Route } from '@angular/router';

export const platformTenantAdminsRoutes: Route[] = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/list/platform-tenant-admins-global.page').then(
        m => m.PlatformTenantAdminsGlobalPage,
      ),
  },
  {
    path: ':userId',
    loadComponent: () =>
      import('./pages/detail/platform-tenant-admin-detail.page').then(
        m => m.PlatformTenantAdminDetailPage,
      ),
  },
];
