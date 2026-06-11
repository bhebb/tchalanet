import { Route } from '@angular/router';

export const cashierRoutes: Route[] = [
  {
    path: '',
    loadComponent: () =>
      import('../dashboard/private-dashboard.page').then(m => m.PrivateDashboardPage),
  },
  {
    path: 'sell',
    loadComponent: () => import('./pages/sell/cashier-sell.page').then(m => m.CashierSellPage),
  },
];
