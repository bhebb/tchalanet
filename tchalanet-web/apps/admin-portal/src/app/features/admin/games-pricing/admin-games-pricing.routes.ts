import { Route } from '@angular/router';

export const adminGamesPricingRoutes: Route[] = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/overview/admin-games-pricing.page').then(m => m.AdminGamesPricingPage),
  },
];
