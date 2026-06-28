import { Route } from '@angular/router';

export const platformOperationsRoutes: Route[] = [
  {
    path: '',
    loadComponent: () =>
      import('../pages/ops/platform-ops.page').then(m => m.PlatformOpsPage),
  },
  {
    path: 'health',
    redirectTo: '/app/platform',
    pathMatch: 'full',
  },
  {
    path: 'batch',
    loadComponent: () =>
      import('../pages/ops/platform-ops-batch.page').then(m => m.PlatformOpsBatchPage),
  },
  {
    path: 'jobs',
    loadComponent: () =>
      import('../pages/ops/platform-ops-batch.page').then(m => m.PlatformOpsBatchPage),
  },
  {
    path: 'schedulers',
    loadComponent: () =>
      import('../pages/ops/platform-ops-batch.page').then(m => m.PlatformOpsBatchPage),
  },
  {
    path: 'providers',
    data: {
      titleKey: 'platform.nav.providers',
      descriptionKey: 'platform.placeholder.descriptions.providers',
      icon: 'cloud_sync',
    },
    loadComponent: () =>
      import('../pages/platform-placeholder.page').then(m => m.PlatformPlaceholderPage),
  },
  {
    path: 'resources',
    data: {
      titleKey: 'dashboard.superadmin.ops.resources.title',
      descriptionKey: 'platform.placeholder.descriptions.opsResources',
      icon: 'memory',
    },
    loadComponent: () =>
      import('../pages/platform-placeholder.page').then(m => m.PlatformPlaceholderPage),
  },
  {
    path: 'archives/purges',
    data: { archiveView: 'purges' },
    loadComponent: () =>
      import('./pages/archive/platform-archive.page').then(m => m.PlatformArchivePage),
  },
  {
    path: 'archives',
    loadComponent: () =>
      import('./pages/archive/platform-archive.page').then(m => m.PlatformArchivePage),
  },
  {
    path: 'audit',
    loadComponent: () =>
      import('./pages/audit/platform-audit.page').then(m => m.PlatformAuditPage),
  },
  {
    path: 'entity-history',
    loadComponent: () =>
      import('./pages/entity-history/platform-entity-history.page').then(
        m => m.PlatformEntityHistoryPage,
      ),
  },
  {
    path: 'draws',
    loadComponent: () =>
      import('../pages/ops/platform-ops-draws.page').then(m => m.PlatformOpsDrawsPage),
  },
  {
    path: 'draw-results',
    loadComponent: () =>
      import('../pages/ops/platform-ops-draw-results.page').then(m => m.PlatformOpsDrawResultsPage),
  },
  {
    path: 'cache',
    loadComponent: () =>
      import('../pages/ops/platform-ops-cache.page').then(m => m.PlatformOpsCachePage),
  },
  {
    path: 'communication-tests',
    loadComponent: () =>
      import('./pages/communication/platform-communication-tests.page').then(
        m => m.PlatformCommunicationTestsPage,
      ),
  },
  {
    path: 'communication',
    loadComponent: () =>
      import('./pages/communication/platform-communication-outbox.page').then(
        m => m.PlatformCommunicationOutboxPage,
      ),
  },
  {
    path: 'identity-sync',
    data: {
      titleKey: 'platform.nav.identitySync',
      descriptionKey: 'platform.placeholder.descriptions.identitySync',
      icon: 'sync_alt',
    },
    loadComponent: () =>
      import('../pages/platform-placeholder.page').then(m => m.PlatformPlaceholderPage),
  },
];
