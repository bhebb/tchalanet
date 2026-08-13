import { Route } from '@angular/router';

export const adminDrawChannelsRoutes: Route[] = [
  {
    path: ':id',
    loadComponent: () =>
      import('./pages/detail/admin-draw-channel-detail.page').then(
        m => m.AdminDrawChannelDetailPage,
      ),
  },
  {
    path: '',
    loadComponent: () =>
      import('./pages/overview/admin-draw-channels.page').then(m => m.AdminDrawChannelsPage),
  },
];
