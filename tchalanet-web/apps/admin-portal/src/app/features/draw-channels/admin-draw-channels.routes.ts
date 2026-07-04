import { Route } from '@angular/router';

export const adminDrawChannelsRoutes: Route[] = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/overview/admin-draw-channels.page').then(m => m.AdminDrawChannelsPage),
  },
];
