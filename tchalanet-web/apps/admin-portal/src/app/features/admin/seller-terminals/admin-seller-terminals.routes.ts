import { Route } from '@angular/router';

export const adminSellerTerminalsRoutes: Route[] = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/list/admin-seller-terminals.page').then(
        m => m.AdminSellerTerminalsPage,
      ),
  },
  {
    path: 'new',
    loadComponent: () =>
      import('./pages/new/admin-seller-terminal-new.page').then(
        m => m.AdminSellerTerminalNewPage,
      ),
  },
  {
    path: 'commissions',
    loadComponent: () =>
      import('../pages/commission/admin-commission.page').then(m => m.AdminCommissionPage),
  },
  {
    path: 'activation',
    loadComponent: () =>
      import('./pages/activation/seller-terminal-activation.page').then(
        m => m.SellerTerminalActivationPage,
      ),
  },
  {
    path: 'sell',
    redirectTo: '/app/admin/pos/sale',
    pathMatch: 'full',
  },
  {
    path: ':sellerTerminalId',
    loadComponent: () =>
      import('./pages/detail/admin-seller-terminal-detail.page').then(
        m => m.AdminSellerTerminalDetailPage,
      ),
  },
  {
    path: ':sellerTerminalId/pos',
    loadComponent: () =>
      import('../../pos/sale/pages/terminal/pos-terminal-sale.page').then(
        m => m.PosTerminalSalePage,
      ),
  },
];
