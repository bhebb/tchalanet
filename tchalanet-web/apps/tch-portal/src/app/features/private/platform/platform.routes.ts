import { Route } from '@angular/router';

export const platformRoutes: Route[] = [
  {
    path: '',
    loadComponent: () =>
      import('../shell/page-model-host/private-dashboard.page').then(m => m.PrivateDashboardPage),
  },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('../shell/page-model-host/private-dashboard.page').then(m => m.PrivateDashboardPage),
  },
  {
    path: 'health',
    loadComponent: () => import('./pages/ops/platform-ops.page').then(m => m.PlatformOpsPage),
  },
  {
    path: 'overview',
    loadComponent: () =>
      import('../shell/page-model-host/private-dashboard.page').then(m => m.PrivateDashboardPage),
  },
  {
    path: 'tenants',
    loadChildren: () =>
      import('./tenants/platform-tenants.routes').then(m => m.platformTenantRoutes),
  },
  { path: 'tenant-provisioning', redirectTo: 'tenants/onboarding', pathMatch: 'full' },
  { path: 'tenant-onboarding', redirectTo: 'tenants/onboarding', pathMatch: 'full' },
  {
    path: 'tenant-admins',
    loadChildren: () =>
      import('./tenant-admins/platform-tenant-admins.routes').then(
        m => m.platformTenantAdminsRoutes,
      ),
  },
  { path: 'subscriptions', redirectTo: 'catalog/plans-pricing', pathMatch: 'full' },
  {
    path: 'entitlements',
    data: {
      titleKey: 'platform.nav.usageRights',
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
    loadComponent: () =>
      import('./pages/archive/platform-archive.page').then(m => m.PlatformArchivePage),
  },
  {
    path: 'ops/audit',
    loadComponent: () => import('./pages/audit/platform-audit.page').then(m => m.PlatformAuditPage),
  },
  {
    path: 'ops/draws',
    loadComponent: () =>
      import('./pages/ops/platform-ops-draws.page').then(m => m.PlatformOpsDrawsPage),
  },
  { path: 'draws', redirectTo: 'ops/draws', pathMatch: 'full' },
  { path: 'draw-channels', redirectTo: 'ops/draws', pathMatch: 'full' },
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
  { path: 'ops/draw-lifecycle', redirectTo: 'ops/draws', pathMatch: 'full' },
  {
    path: '_ops/draw-lifecycle',
    loadComponent: () =>
      import('./pages/ops/platform-ops-draw-lifecycle.page').then(
        m => m.PlatformOpsDrawLifecyclePage,
      ),
  },
  {
    path: 'catalog/settings',
    loadComponent: () =>
      import('./pages/catalog/platform-catalog-settings.page').then(m => m.PlatformCatalogSettingsPage),
  },
  { path: 'settings', redirectTo: 'catalog/settings', pathMatch: 'full' },
  { path: 'theme-presets', redirectTo: 'catalog/themes', pathMatch: 'full' },
  {
    path: 'catalog/themes',
    loadComponent: () =>
      import('./pages/catalog/platform-catalog-themes.page').then(m => m.PlatformCatalogThemesPage),
  },
  { path: 'referentials', redirectTo: 'catalog/games', pathMatch: 'full' },
  {
    path: 'catalog/games',
    loadComponent: () =>
      import('./pages/catalog/platform-catalog-games.page').then(m => m.PlatformCatalogGamesPage),
  },
  {
    path: 'catalog/pricing',
    loadComponent: () =>
      import('./pages/catalog/platform-catalog-pricing.page').then(m => m.PlatformCatalogPricingPage),
  },
  {
    path: 'catalog/draw-channels',
    loadComponent: () =>
      import('./pages/catalog/platform-catalog-draw-channels.page').then(m => m.PlatformCatalogDrawChannelsPage),
  },
  {
    path: 'catalog/result-slots',
    loadComponent: () =>
      import('./pages/catalog/platform-catalog-result-slots.page').then(m => m.PlatformCatalogResultSlotsPage),
  },
  {
    path: 'catalog/plans-pricing',
    loadComponent: () =>
      import('./pages/catalog/platform-catalog-plans.page').then(m => m.PlatformCatalogPlansPage),
  },
  {
    path: 'catalog/translations',
    loadComponent: () =>
      import('./pages/catalog/platform-catalog-i18n-overrides.page').then(m => m.PlatformCatalogI18nOverridesPage),
  },
  { path: 'i18n', redirectTo: 'catalog/translations', pathMatch: 'full' },
  {
    path: 'catalog/page-model-templates',
    loadComponent: () =>
      import('./pages/catalog/platform-catalog-page-model-templates.page').then(m => m.PlatformCatalogPageModelTemplatesPage),
  },
  { path: 'pagemodels', redirectTo: 'catalog/page-model-templates', pathMatch: 'full' },
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
    path: 'communication/contacts',
    loadComponent: () =>
      import('./pages/contact-requests/platform-contact-requests.page').then(
        m => m.PlatformContactRequestsPage,
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
    path: 'super-admins',
    loadChildren: () =>
      import('./super-admins/platform-super-admins.routes').then(m => m.platformSuperAdminsRoutes),
  },
  { path: 'access/super-admins', redirectTo: 'super-admins', pathMatch: 'full' },
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
