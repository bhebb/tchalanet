import { Route } from '@angular/router';

export const adminGeneratedDrawsRoutes: Route[] = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/overview/admin-generated-draws.page').then(
        m => m.AdminGeneratedDrawsPage,
      ),
  },
];
