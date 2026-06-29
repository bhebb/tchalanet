import { Route } from '@angular/router';
import { LoginPage } from '@tch/core/auth';

export const appRoutes: Route[] = [
  {
    path: 'login',
    component: LoginPage,
  },
  {
    path: '',
    loadComponent: () =>
      import('./features/public/shell/public-shell.component').then(m => m.TchPublicShellComponent),
    loadChildren: () => import('./features/public/public.routes').then(m => m.publicRoutes),
  },
];
