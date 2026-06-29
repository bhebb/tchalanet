import { Route } from '@angular/router';

export const platformRoutes: Route[] = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/platform-placeholder.page').then(m => m.PlatformPlaceholderPage),
    data: {
      titleKey: 'platform.nav.dashboard',
      descriptionKey: 'platform.placeholder.description',
      icon: 'dashboard',
    },
  },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./pages/platform-placeholder.page').then(m => m.PlatformPlaceholderPage),
    data: {
      titleKey: 'platform.nav.dashboard',
      descriptionKey: 'platform.placeholder.description',
      icon: 'dashboard',
    },
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
  {
    path: 'audit',
    loadComponent: () =>
      import('./operations/pages/audit/platform-audit.page').then(m => m.PlatformAuditPage),
  },
  { path: 'audit/logs', redirectTo: 'audit', pathMatch: 'full' },
  {
    path: 'audit/entity-history',
    loadComponent: () =>
      import('./operations/pages/entity-history/platform-entity-history.page').then(
        m => m.PlatformEntityHistoryPage,
      ),
  },
  {
    path: 'archives',
    loadComponent: () =>
      import('./operations/pages/archive/platform-archive.page').then(m => m.PlatformArchivePage),
  },
  {
    path: 'archives/runs',
    loadComponent: () =>
      import('./operations/pages/archive/platform-archive.page').then(m => m.PlatformArchivePage),
  },
  {
    path: 'archives/issues',
    loadComponent: () =>
      import('./operations/pages/archive/platform-archive.page').then(m => m.PlatformArchivePage),
  },
  {
    path: 'archives/legal-holds',
    loadComponent: () =>
      import('./operations/pages/archive/platform-archive.page').then(m => m.PlatformArchivePage),
  },
  {
    path: 'archives/partitions',
    loadComponent: () =>
      import('./operations/pages/archive/platform-archive.page').then(m => m.PlatformArchivePage),
  },
  { path: 'ops/draw-lifecycle', redirectTo: 'ops/draws', pathMatch: 'full' },
  {
    path: '_ops/draw-lifecycle',
    loadComponent: () =>
      import('./operations/pages/ops/platform-ops-draw-lifecycle.page').then(
        m => m.PlatformOpsDrawLifecyclePage,
      ),
  },
  // ── Catalog / Referentials ─────────────────────────────────────────────────
  {
    path: 'catalog',
    loadChildren: () =>
      import('./catalog/platform-catalog.routes').then(m => m.platformCatalogRoutes),
  },
  { path: 'subscriptions', redirectTo: 'catalog/plans', pathMatch: 'full' },
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
    loadComponent: () =>
      import('./access/pages/platform-permissions.page').then(m => m.PlatformPermissionsPage),
  },
  {
    path: 'access/roles',
    loadComponent: () =>
      import('./access/pages/platform-roles.page').then(m => m.PlatformRolesPage),
  },
  {
    path: 'access/users',
    loadComponent: () =>
      import('./access/pages/platform-access-users.page').then(m => m.PlatformAccessUsersPage),
  },
  {
    path: 'super-admins',
    loadChildren: () =>
      import('./super-admins/platform-super-admins.routes').then(m => m.platformSuperAdminsRoutes),
  },
  { path: 'access/super-admins', redirectTo: 'super-admins', pathMatch: 'full' },
  { path: 'access/overrides', redirectTo: 'super-admins', pathMatch: 'full' },
  { path: 'access/admin-accounts', redirectTo: 'tenant-admins', pathMatch: 'full' },
  {
    path: 'access/backend-keys',
    data: {
      titleKey: 'platform.nav.backendKeys',
      descriptionKey: 'platform.placeholder.descriptions.backendKeys',
      icon: 'vpn_key',
    },
    loadComponent: () =>
      import('./pages/platform-placeholder.page').then(m => m.PlatformPlaceholderPage),
  },
  // ── Support tenant ─────────────────────────────────────────────────────────
  {
    path: 'support-tenant',
    loadComponent: () =>
      import('./pages/support-tenant/platform-support-tenant.page').then(
        m => m.PlatformSupportTenantPage,
      ),
  },
  // ── Tchala ─────────────────────────────────────────────────────────────────
  {
    path: 'tchala/suggestions',
    data: {
      titleKey: 'platform.nav.tchalaSuggestions',
      descriptionKey: 'platform.placeholder.descriptions.tchalaSuggestions',
      icon: 'lightbulb',
    },
    loadComponent: () =>
      import('./pages/platform-placeholder.page').then(m => m.PlatformPlaceholderPage),
  },
  {
    path: 'tchala/import',
    data: {
      titleKey: 'platform.nav.tchalaImport',
      descriptionKey: 'platform.placeholder.descriptions.tchalaImport',
      icon: 'upload_file',
    },
    loadComponent: () =>
      import('./pages/platform-placeholder.page').then(m => m.PlatformPlaceholderPage),
  },
  {
    path: 'tchala/cleanup',
    data: {
      titleKey: 'platform.nav.tchalaCleanup',
      descriptionKey: 'platform.placeholder.descriptions.tchalaCleanup',
      icon: 'auto_fix_high',
    },
    loadComponent: () =>
      import('./pages/platform-placeholder.page').then(m => m.PlatformPlaceholderPage),
  },
  { path: 'tchala', redirectTo: 'tchala/suggestions', pathMatch: 'full' },
  // ── Communication / Support & contenu ──────────────────────────────────────
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
  {
    path: 'communication/config',
    loadComponent: () =>
      import('./pages/contact-config/platform-contact-config.page').then(
        m => m.PlatformContactConfigPage,
      ),
  },
  {
    path: 'communication/outbox',
    loadComponent: () =>
      import('./operations/pages/communication/platform-communication-outbox.page').then(
        m => m.PlatformCommunicationOutboxPage,
      ),
  },
  {
    path: 'communication/tests',
    loadComponent: () =>
      import('./operations/pages/communication/platform-communication-tests.page').then(
        m => m.PlatformCommunicationTestsPage,
      ),
  },
  {
    path: 'contact-config',
    loadComponent: () =>
      import('./pages/contact-config/platform-contact-config.page').then(
        m => m.PlatformContactConfigPage,
      ),
  },
  // ── Reports / misc ─────────────────────────────────────────────────────────
  {
    path: 'reports',
    data: {
      titleKey: 'platform.nav.platformReports',
      descriptionKey: 'platform.placeholder.descriptions.platformReports',
      icon: 'bar_chart',
    },
    loadComponent: () =>
      import('./pages/platform-placeholder.page').then(m => m.PlatformPlaceholderPage),
  },
  {
    path: 'releases',
    loadComponent: () =>
      import('./pages/news/platform-news.page').then(m => m.PlatformNewsPage),
  },
];
