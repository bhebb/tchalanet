import { Route } from '@angular/router';

export const cashierRoutes: Route[] = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/dashboard/cashier-dashboard.page').then(m => m.CashierDashboardPage),
  },
  {
    path: 'sell',
    loadComponent: () => import('./pages/sell/cashier-sell.page').then(m => m.CashierSellPage),
  },
];
