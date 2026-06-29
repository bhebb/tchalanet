import { Route } from '@angular/router';

export const appRoutes: Route[] = [
  {
    path: 'login',
    loadComponent: () => import('@tch/core/auth').then(m => m.TchLoginPage),
  },
  {
    path: '',
    loadChildren: () => import('./features/platform/platform.routes').then(m => m.platformRoutes),
  },
  {
    path: '**',
    redirectTo: '',
  },
];
