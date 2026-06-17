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
    path: 'terminals',
    loadComponent: () =>
      import('./pages/terminals/admin-terminals.page').then(m => m.AdminTerminalsPage),
  },
  {
    path: 'games',
    loadComponent: () =>
      import('./pages/games/admin-games.page').then(m => m.AdminGamesPage),
  },
  {
    path: 'business-days',
    loadComponent: () =>
      import('./pages/business-days/admin-business-days.page').then(m => m.AdminBusinessDaysPage),
  },
  {
    path: 'commission',
    loadComponent: () =>
      import('./pages/commission/admin-commission.page').then(m => m.AdminCommissionPage),
  },
  {
    path: 'pricing',
    loadComponent: () =>
      import('./pages/pricing/admin-pricing.page').then(m => m.AdminPricingPage),
  },
  {
    path: 'draws',
    loadComponent: () =>
      import('./pages/draws/admin-draws.page').then(m => m.AdminDrawsPage),
  },
  {
    path: 'draw-results',
    loadComponent: () =>
      import('./pages/draw-results/admin-draw-results.page').then(m => m.AdminDrawResultsPage),
  },
  {
    path: 'payouts',
    loadComponent: () =>
      import('./pages/payouts/admin-payouts.page').then(m => m.AdminPayoutsPage),
  },
  {
    path: 'subscription',
    loadComponent: () =>
      import('./pages/subscription/admin-subscription.page').then(m => m.AdminSubscriptionPage),
  },
  {
    path: 'settings',
    loadComponent: () =>
      import('./pages/settings/admin-settings.page').then(m => m.AdminSettingsPage),
  },
  {
    path: 'settings/runtime',
    loadComponent: () =>
      import('./pages/settings/admin-runtime.page').then(m => m.AdminRuntimePage),
  },
  {
    path: 'settings/config',
    loadComponent: () =>
      import('./pages/settings/admin-config.page').then(m => m.AdminConfigPage),
  },
];
