import { Route } from '@angular/router';

export const platformOperationsRoutes: Route[] = [
  {
    path: '',
    loadComponent: () =>
      import('../pages/ops/platform-ops.page').then(m => m.PlatformOpsPage),
  },
  {
    path: 'health',
    loadComponent: () =>
      import('../pages/ops/platform-ops.page').then(m => m.PlatformOpsPage),
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
];
