import { Route } from '@angular/router';

export const platformRoutes: Route[] = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/dashboard/platform-dashboard.page').then(m => m.PlatformDashboardPage),
  },
  {
    path: 'tenants',
    loadComponent: () =>
      import('./pages/tenants/platform-tenants.page').then(m => m.PlatformTenantsPage),
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
];
