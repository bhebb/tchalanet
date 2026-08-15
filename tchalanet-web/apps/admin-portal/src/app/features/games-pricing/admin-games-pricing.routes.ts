import { Route } from '@angular/router';

export const adminGamesPricingRoutes: Route[] = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () =>
      import('./pages/overview/admin-games-pricing.page').then(m => m.AdminGamesPricingPage),
  },
  {
    path: 'channel-matrix',
    loadComponent: () =>
      import('../draw-sales-matrix/pages/admin-draw-sales-matrix.page').then(
        m => m.AdminDrawSalesMatrixPage,
      ),
  },
  {
    path: ':gameCode/settings',
    loadComponent: () =>
      import('./pages/settings/admin-game-settings.page').then(m => m.AdminGameSettingsPage),
  },
];
