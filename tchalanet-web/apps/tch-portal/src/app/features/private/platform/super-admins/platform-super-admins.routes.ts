import { Route } from '@angular/router';

export const platformSuperAdminsRoutes: Route[] = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/super-admins/platform-admin-users.page').then(m => m.PlatformAdminUsersPage),
  },
  {
    path: ':userId',
    loadComponent: () =>
      import('./pages/admin-user-detail/platform-admin-user-detail.page').then(
        m => m.PlatformAdminUserDetailPage,
      ),
  },
];
