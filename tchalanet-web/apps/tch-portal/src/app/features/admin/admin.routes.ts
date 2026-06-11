import { Route } from '@angular/router';

export const adminRoutes: Route[] = [
  {
    path: '',
    loadComponent: () =>
      import('../dashboard/private-dashboard.page').then(m => m.PrivateDashboardPage),
  },
  {
    path: 'onboarding',
    loadComponent: () =>
      import('./pages/onboarding/admin-onboarding.page').then(m => m.AdminOnboardingPage),
  },
  {
    path: 'users',
    loadComponent: () => import('./pages/users/admin-users.page').then(m => m.AdminUsersPage),
  },
  {
    path: 'sellers',
    loadComponent: () =>
      import('./pages/sellers/admin-sellers.page').then(m => m.AdminSellersPage),
  },
  {
    path: 'outlets',
    loadComponent: () =>
      import('./pages/outlets/admin-outlets.page').then(m => m.AdminOutletsPage),
  },
  {
    path: 'terminals',
    loadComponent: () =>
      import('./pages/terminals/admin-terminals.page').then(m => m.AdminTerminalsPage),
  },
  {
    path: 'sessions',
    loadComponent: () =>
      import('./pages/sessions/admin-sessions.page').then(m => m.AdminSessionsPage),
  },
  {
    path: 'settings',
    loadComponent: () =>
      import('./pages/settings/admin-settings.page').then(m => m.AdminSettingsPage),
  },
];
