import { Route } from '@angular/router';

export const sellerTerminalRoutes: Route[] = [
  {
    path: '',
    loadComponent: () =>
      import('../shell/page-model-host/private-dashboard.page').then(m => m.PrivateDashboardPage),
  },
  {
    path: 'sell',
    loadComponent: () => import('./pages/sell/pos-sell.page').then(m => m.PosSellPage),
  },
];
