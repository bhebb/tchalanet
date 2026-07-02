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
  {
    path: 'tickets/:ticketId',
    loadComponent: () =>
      import('./sale/pages/ticket-detail/pos-ticket-detail.page').then(m => m.PosTicketDetailPage),
  },
  {
    path: 'verify',
    loadComponent: () =>
      import('./sale/pages/verify/pos-ticket-verify.page').then(m => m.PosTicketVerifyPage),
  },
];
