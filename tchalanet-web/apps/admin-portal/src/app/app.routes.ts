import { Route } from '@angular/router';

export const appRoutes: Route[] = [
  {
    path: 'login',
    loadComponent: () => import('@tch/core/auth').then(m => m.TchLoginPage),
  },
  {
    path: 'pos',
    loadChildren: () => import('./features/pos/pos.routes').then(m => m.posRoutes),
  },
  {
    path: '',
    loadChildren: () => import('./features/admin/admin.routes').then(m => m.adminRoutes),
  },
  {
    path: '**',
    redirectTo: '',
  },
];
