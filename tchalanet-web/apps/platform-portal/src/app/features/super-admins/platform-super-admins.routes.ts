import { Route } from '@angular/router';

export const platformSuperAdminsRoutes: Route[] = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/super-admins/platform-super-admins.page').then(m => m.PlatformSuperAdminsPage),
  },
  {
    path: ':userId',
    loadComponent: () =>
      import('./pages/admin-user-detail/platform-super-admin-detail.page').then(
        m => m.PlatformSuperAdminDetailPage,
      ),
  },
];
