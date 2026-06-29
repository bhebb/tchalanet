import { Route } from '@angular/router';

export const consoleProfileRoutes: Route[] = [
  {
    path: '',
    loadComponent: () => import('./pages/profile/profile.page').then(m => m.ProfilePage),
  },
];
