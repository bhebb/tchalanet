import { Routes } from '@angular/router';

export const posRoutes: Routes = [
  {
    path: '',
    redirectTo: 'sale',
    pathMatch: 'full',
  },
  {
    path: 'sale',
    loadComponent: () => import('./sale/pages/sell/pos-sell.page').then(m => m.PosSellPage),
  },
  {
    path: 'sale/:sellerTerminalId',
    loadComponent: () =>
      import('./sale/pages/terminal/pos-terminal-sale.page').then(m => m.PosTerminalSalePage),
  },
];
