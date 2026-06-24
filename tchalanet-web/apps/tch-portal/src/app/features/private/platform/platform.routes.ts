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
    path: 'overview',
    loadComponent: () =>
      import('../shell/page-model-host/private-dashboard.page').then(m => m.PrivateDashboardPage),
  },
  // ── Tenants ────────────────────────────────────────────────────────────────
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
  // ── Operations ─────────────────────────────────────────────────────────────
  {
    path: 'ops',
    loadChildren: () =>
      import('./operations/platform-operations.routes').then(m => m.platformOperationsRoutes),
  },
  { path: 'health', redirectTo: 'ops/health', pathMatch: 'full' },
  { path: 'draws', redirectTo: 'ops/draws', pathMatch: 'full' },
  { path: 'draw-channels', redirectTo: 'ops/draws', pathMatch: 'full' },
  { path: 'audit', redirectTo: 'ops/audit', pathMatch: 'full' },
  { path: 'ops/draw-lifecycle', redirectTo: 'ops/draws', pathMatch: 'full' },
  {
    path: '_ops/draw-lifecycle',
    loadComponent: () =>
      import('./pages/ops/platform-ops-draw-lifecycle.page').then(
        m => m.PlatformOpsDrawLifecyclePage,
      ),
  },
  // ── Catalog / Referentials ─────────────────────────────────────────────────
  {
    path: 'catalog',
    loadChildren: () =>
      import('./catalog/platform-catalog.routes').then(m => m.platformCatalogRoutes),
  },
  { path: 'subscriptions', redirectTo: 'catalog/plans-pricing', pathMatch: 'full' },
  { path: 'settings', redirectTo: 'catalog/settings', pathMatch: 'full' },
  { path: 'theme-presets', redirectTo: 'catalog/themes', pathMatch: 'full' },
  { path: 'referentials', redirectTo: 'catalog/games', pathMatch: 'full' },
  { path: 'i18n', redirectTo: 'catalog/translations', pathMatch: 'full' },
  { path: 'pagemodels', redirectTo: 'catalog/page-model-templates', pathMatch: 'full' },
  // ── Access rights ──────────────────────────────────────────────────────────
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
  // ── Communication ──────────────────────────────────────────────────────────
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
    loadComponent: () =>
      import('./pages/news/platform-news.page').then(m => m.PlatformNewsPage),
  },
  // ── Reports / misc ─────────────────────────────────────────────────────────
  {
    path: 'reports',
    loadComponent: () =>
      import('./operations/pages/audit/platform-audit.page').then(m => m.PlatformAuditPage),
  },
  {
    path: 'releases',
    loadComponent: () =>
      import('./pages/news/platform-news.page').then(m => m.PlatformNewsPage),
  },
];
