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
    path: ':sellerTerminalId',
    loadComponent: () =>
      import('../pages/admin-placeholder.page').then(m => m.AdminPlaceholderPage),
    data: { titleKey: 'admin.sellerTerminals.title', icon: 'point_of_sale' },
  },
  {
    path: ':sellerTerminalId/pos',
    loadComponent: () =>
      import('./pages/pos/admin-seller-terminal-pos.page').then(
        m => m.AdminSellerTerminalPosPage,
      ),
  },
];
