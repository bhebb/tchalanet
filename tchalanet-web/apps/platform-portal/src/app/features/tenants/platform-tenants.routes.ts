import { Route } from '@angular/router';

export const platformTenantRoutes: Route[] = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/list/platform-tenants.page').then(m => m.PlatformTenantsPage),
  },
  {
    path: 'onboarding',
    loadComponent: () =>
      import('./pages/onboarding/platform-tenant-provisioning.page').then(
        m => m.PlatformTenantProvisioningPage,
      ),
  },
  { path: 'new', redirectTo: 'onboarding', pathMatch: 'full' },
  {
    path: ':tenantId/admins/new',
    loadComponent: () =>
      import('./pages/admin-create/platform-tenant-admin-create.page').then(
        m => m.PlatformTenantAdminCreatePage,
      ),
  },
  {
    path: ':tenantId/admins',
    loadComponent: () =>
      import('./pages/admins/platform-tenant-admins.page').then(m => m.PlatformTenantAdminsPage),
  },
  {
    path: ':tenantId',
    loadComponent: () =>
      import('./pages/detail/platform-tenant-detail.page').then(m => m.PlatformTenantDetailPage),
  },
];
