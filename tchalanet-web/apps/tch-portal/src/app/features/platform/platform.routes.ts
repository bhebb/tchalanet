import { Route } from '@angular/router';

export const platformRoutes: Route[] = [
  {
    path: '',
    loadComponent: () =>
      import('../dashboard/private-dashboard.page').then(m => m.PrivateDashboardPage),
  },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('../dashboard/private-dashboard.page').then(m => m.PrivateDashboardPage),
  },
  {
    path: 'health',
    loadComponent: () => import('./pages/ops/platform-ops.page').then(m => m.PlatformOpsPage),
  },
  {
    path: 'overview',
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
    path: 'tenants/onboarding',
    loadComponent: () =>
      import('./pages/tenant-provisioning/platform-tenant-provisioning.page').then(
        m => m.PlatformTenantProvisioningPage,
      ),
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
    loadComponent: () =>
      import('./pages/tenants/platform-tenant-admins.page').then(m => m.PlatformTenantAdminsPage),
  },
  {
    path: 'tenant-provisioning',
    loadComponent: () =>
      import('./pages/tenant-provisioning/platform-tenant-provisioning.page').then(
        m => m.PlatformTenantProvisioningPage,
      ),
  },
  {
    path: 'tenant-onboarding',
    loadComponent: () =>
      import('./pages/tenant-provisioning/platform-tenant-provisioning.page').then(
        m => m.PlatformTenantProvisioningPage,
      ),
  },
  {
    path: 'tenant-admins',
    loadComponent: () =>
      import('./pages/tenants/platform-tenant-admins.page').then(m => m.PlatformTenantAdminsPage),
  },
  {
    path: 'subscriptions',
    loadComponent: () =>
      import('./pages/ops/platform-ops-settings.page').then(m => m.PlatformOpsSettingsPage),
  },
  {
    path: 'entitlements',
    data: {
      titleKey: 'platform.nav.entitlements',
      descriptionKey: 'platform.placeholder.descriptions.entitlements',
      icon: 'verified_user',
    },
    loadComponent: () =>
      import('./pages/platform-placeholder.page').then(m => m.PlatformPlaceholderPage),
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
    loadComponent: () => import('./pages/news/platform-news.page').then(m => m.PlatformNewsPage),
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
    path: 'ops/health',
    loadComponent: () => import('./pages/ops/platform-ops.page').then(m => m.PlatformOpsPage),
  },
  {
    path: 'ops/batch',
    loadComponent: () =>
      import('./pages/ops/platform-ops-batch.page').then(m => m.PlatformOpsBatchPage),
  },
  {
    path: 'ops/jobs',
    loadComponent: () =>
      import('./pages/ops/platform-ops-batch.page').then(m => m.PlatformOpsBatchPage),
  },
  {
    path: 'ops/schedulers',
    loadComponent: () =>
      import('./pages/ops/platform-ops-batch.page').then(m => m.PlatformOpsBatchPage),
  },
  {
    path: 'ops/providers',
    data: {
      titleKey: 'platform.nav.providers',
      descriptionKey: 'platform.placeholder.descriptions.providers',
      icon: 'cloud_sync',
    },
    loadComponent: () =>
      import('./pages/platform-placeholder.page').then(m => m.PlatformPlaceholderPage),
  },
  {
    path: 'ops/archives',
    data: {
      titleKey: 'platform.nav.archives',
      descriptionKey: 'platform.placeholder.descriptions.archives',
      icon: 'inventory_2',
    },
    loadComponent: () =>
      import('./pages/platform-placeholder.page').then(m => m.PlatformPlaceholderPage),
  },
  {
    path: 'ops/audit',
    loadComponent: () => import('./pages/audit/platform-audit.page').then(m => m.PlatformAuditPage),
  },
  {
    path: 'draws',
    loadComponent: () =>
      import('./pages/ops/platform-ops-draws.page').then(m => m.PlatformOpsDrawsPage),
  },
  {
    path: 'draw-channels',
    loadComponent: () =>
      import('./pages/ops/platform-ops-draws.page').then(m => m.PlatformOpsDrawsPage),
  },
  {
    path: 'ops/draw-results',
    loadComponent: () =>
      import('./pages/ops/platform-ops-draw-results.page').then(m => m.PlatformOpsDrawResultsPage),
  },
  {
    path: 'ops/cache',
    loadComponent: () =>
      import('./pages/ops/platform-ops-cache.page').then(m => m.PlatformOpsCachePage),
  },
  {
    path: 'ops/draw-lifecycle',
    loadComponent: () =>
      import('./pages/ops/platform-ops-draw-lifecycle.page').then(
        m => m.PlatformOpsDrawLifecyclePage,
      ),
  },
  {
    path: 'settings',
    loadComponent: () =>
      import('./pages/ops/platform-ops-settings.page').then(m => m.PlatformOpsSettingsPage),
  },
  {
    path: 'catalog/settings',
    loadComponent: () =>
      import('./pages/ops/platform-ops-settings.page').then(m => m.PlatformOpsSettingsPage),
  },
  {
    path: 'theme-presets',
    loadComponent: () =>
      import('./pages/ops/platform-ops-settings.page').then(m => m.PlatformOpsSettingsPage),
  },
  {
    path: 'catalog/themes',
    loadComponent: () =>
      import('./pages/ops/platform-ops-settings.page').then(m => m.PlatformOpsSettingsPage),
  },
  {
    path: 'referentials',
    loadComponent: () =>
      import('./pages/ops/platform-ops-settings.page').then(m => m.PlatformOpsSettingsPage),
  },
  {
    path: 'catalog/games',
    data: {
      titleKey: 'platform.nav.games',
      descriptionKey: 'platform.placeholder.descriptions.games',
      icon: 'casino',
    },
    loadComponent: () =>
      import('./pages/platform-placeholder.page').then(m => m.PlatformPlaceholderPage),
  },
  {
    path: 'catalog/draw-channels',
    loadComponent: () =>
      import('./pages/ops/platform-ops-draws.page').then(m => m.PlatformOpsDrawsPage),
  },
  {
    path: 'catalog/result-slots',
    data: {
      titleKey: 'platform.nav.resultSlots',
      descriptionKey: 'platform.placeholder.descriptions.resultSlots',
      icon: 'view_timeline',
    },
    loadComponent: () =>
      import('./pages/platform-placeholder.page').then(m => m.PlatformPlaceholderPage),
  },
  {
    path: 'catalog/plans-pricing',
    loadComponent: () =>
      import('./pages/ops/platform-ops-settings.page').then(m => m.PlatformOpsSettingsPage),
  },
  {
    path: 'i18n',
    loadComponent: () =>
      import('./pages/ops/platform-ops-i18n.page').then(m => m.PlatformOpsI18nPage),
  },
  {
    path: 'catalog/translations',
    loadComponent: () =>
      import('./pages/ops/platform-ops-i18n.page').then(m => m.PlatformOpsI18nPage),
  },
  {
    path: 'pagemodels',
    loadComponent: () =>
      import('./pages/ops/platform-ops-pagemodels.page').then(m => m.PlatformOpsPageModelsPage),
  },
  {
    path: 'catalog/page-model-templates',
    loadComponent: () =>
      import('./pages/ops/platform-ops-pagemodels.page').then(m => m.PlatformOpsPageModelsPage),
  },
  {
    path: 'audit',
    loadComponent: () => import('./pages/audit/platform-audit.page').then(m => m.PlatformAuditPage),
  },
  {
    path: 'communications',
    loadComponent: () =>
      import('./pages/notifications/platform-notifications.page').then(
        m => m.PlatformNotificationsPage,
      ),
  },
  {
    path: 'communication/notifications',
    loadComponent: () =>
      import('./pages/notifications/platform-notifications.page').then(
        m => m.PlatformNotificationsPage,
      ),
  },
  {
    path: 'communication/news',
    loadComponent: () => import('./pages/news/platform-news.page').then(m => m.PlatformNewsPage),
  },
  {
    path: 'access/permissions',
    data: {
      titleKey: 'platform.nav.permissions',
      descriptionKey: 'platform.placeholder.descriptions.permissions',
      icon: 'key',
    },
    loadComponent: () =>
      import('./pages/platform-placeholder.page').then(m => m.PlatformPlaceholderPage),
  },
  {
    path: 'access/roles',
    data: {
      titleKey: 'platform.nav.roles',
      descriptionKey: 'platform.placeholder.descriptions.roles',
      icon: 'groups',
    },
    loadComponent: () =>
      import('./pages/platform-placeholder.page').then(m => m.PlatformPlaceholderPage),
  },
  {
    path: 'access/overrides',
    data: {
      titleKey: 'platform.nav.superAdminOverrides',
      descriptionKey: 'platform.placeholder.descriptions.overrides',
      icon: 'security',
    },
    loadComponent: () =>
      import('./pages/platform-placeholder.page').then(m => m.PlatformPlaceholderPage),
  },
  {
    path: 'reports',
    loadComponent: () => import('./pages/audit/platform-audit.page').then(m => m.PlatformAuditPage),
  },
  {
    path: 'releases',
    loadComponent: () => import('./pages/news/platform-news.page').then(m => m.PlatformNewsPage),
  },
];
