import { Route } from '@angular/router';

export const profileRoutes: Route[] = [
  {
    path: '',
    loadComponent: () => import('./pages/profile/profile.page').then(m => m.ProfilePage),
  },
];
