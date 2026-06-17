import { Route } from '@angular/router';

export const platformRoutes: Route[] = [
  {
    path: '',
    loadComponent: () =>
      import('../dashboard/private-dashboard.page').then(m => m.PrivateDashboardPage),
  },
  {
    path: 'tenants',
    loadComponent: () =>
      import('./pages/tenants/platform-tenants.page').then(m => m.PlatformTenantsPage),
  },
  {
    path: 'tenants/new',
    loadComponent: () =>
      import('./pages/tenants/platform-tenant-create.page').then(m => m.PlatformTenantCreatePage),
  },
  {
    path: 'tenants/:tenantId/admins',
    loadComponent: () =>
      import('./pages/tenants/platform-tenant-admins.page').then(m => m.PlatformTenantAdminsPage),
  },
  {
    path: 'tenants/:tenantId/admins/new',
    loadComponent: () =>
      import('./pages/tenants/platform-tenant-admin-create.page').then(
        m => m.PlatformTenantAdminCreatePage,
      ),
  },
  {
    path: 'tenants/:tenantId',
    redirectTo: 'tenants/:tenantId/admins',
  },
  {
    path: 'tenant-provisioning',
    loadComponent: () =>
      import('./pages/tenant-provisioning/platform-tenant-provisioning.page').then(
        m => m.PlatformTenantProvisioningPage,
      ),
  },
  {
    path: 'contact-requests',
    loadComponent: () =>
      import('./pages/contact-requests/platform-contact-requests.page').then(
        m => m.PlatformContactRequestsPage,
      ),
  },
  {
    path: 'news',
    loadComponent: () =>
      import('./pages/news/platform-news.page').then(m => m.PlatformNewsPage),
  },
  {
    path: 'notifications',
    loadComponent: () =>
      import('./pages/notifications/platform-notifications.page').then(
        m => m.PlatformNotificationsPage,
      ),
  },
  {
    path: 'ops',
    loadComponent: () => import('./pages/ops/platform-ops.page').then(m => m.PlatformOpsPage),
  },
  {
    path: 'ops/batch',
    loadComponent: () =>
      import('./pages/ops/platform-ops-batch.page').then(m => m.PlatformOpsBatchPage),
  },
  {
    path: 'ops/draws',
    loadComponent: () =>
      import('./pages/ops/platform-ops-draws.page').then(m => m.PlatformOpsDrawsPage),
  },
  {
    path: 'ops/draw-results',
    loadComponent: () =>
      import('./pages/ops/platform-ops-draw-results.page').then(
        m => m.PlatformOpsDrawResultsPage,
      ),
  },
  {
    path: 'ops/cache',
    loadComponent: () =>
      import('./pages/ops/platform-ops-cache.page').then(m => m.PlatformOpsCachePage),
  },
  {
    path: 'audit',
    loadComponent: () =>
      import('./pages/audit/platform-audit.page').then(m => m.PlatformAuditPage),
  },
];
