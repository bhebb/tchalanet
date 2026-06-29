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
    loadComponent: () =>
      import('../draw-sales-matrix/pages/admin-draw-sales-matrix.page').then(
        m => m.AdminDrawSalesMatrixPage,
      ),
  },
  {
    path: 'channels',
    loadComponent: () =>
      import('../draw-channels/pages/overview/admin-draw-channels.page').then(
        m => m.AdminDrawChannelsPage,
      ),
  },
];
