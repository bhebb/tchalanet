import { Route } from '@angular/router';

export const adminBusinessProfileRoutes: Route[] = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/overview/admin-business-profile.page').then(
        m => m.AdminBusinessProfilePage,
      ),
  },
];
