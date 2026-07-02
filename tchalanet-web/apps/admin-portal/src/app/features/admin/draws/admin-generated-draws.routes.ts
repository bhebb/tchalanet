import { Route } from '@angular/router';

export const adminGeneratedDrawsRoutes: Route[] = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/overview/admin-generated-draws.page').then(
        m => m.AdminGeneratedDrawsPage,
      ),
  },
  {
    path: 'matrix',
    redirectTo: '/app/admin/games/channel-matrix',
    pathMatch: 'full',
  },
  {
    path: 'results/:resultId',
    loadComponent: () =>
      import('../pages/draw-results/detail/admin-draw-result-detail.page').then(
        m => m.AdminDrawResultDetailPage,
      ),
  },
  {
    path: 'results',
    loadComponent: () =>
      import('../pages/draw-results/admin-draw-results.page').then(
        m => m.AdminDrawResultsPage,
      ),
  },
  {
    path: 'channels',
    loadComponent: () =>
      import('../draw-channels/pages/overview/admin-draw-channels.page').then(
        m => m.AdminDrawChannelsPage,
      ),
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./pages/detail/admin-draw-detail.page').then(
        m => m.AdminDrawDetailPage,
      ),
  },
];
