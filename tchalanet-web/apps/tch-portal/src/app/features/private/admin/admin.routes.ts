import { Route } from '@angular/router';

export const adminRoutes: Route[] = [
  {
    path: '',
    loadComponent: () =>
      import('../shell/page-model-host/private-dashboard.page').then(m => m.PrivateDashboardPage),
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
    path: 'seller-terminals',
    loadComponent: () =>
      import('./pages/seller-terminals/admin-seller-terminals.page').then(
        m => m.AdminSellerTerminalsPage,
      ),
  },
  {
    path: 'games',
    loadComponent: () =>
      import('./pages/games/admin-games.page').then(m => m.AdminGamesPage),
  },
  {
    path: 'games-pricing',
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
    path: 'controls',
    loadComponent: () =>
      import('./pages/commission/admin-commission.page').then(m => m.AdminCommissionPage),
  },
  {
    path: 'controls/commission',
    loadComponent: () =>
      import('./pages/commission/admin-commission.page').then(m => m.AdminCommissionPage),
  },
  {
    path: 'controls/baremes',
    loadComponent: () =>
      import('./pages/controls/admin-baremes.page').then(m => m.AdminBaremesPage),
  },
  {
    path: 'controls/limits',
    loadComponent: () =>
      import('./pages/pricing/admin-pricing.page').then(m => m.AdminPricingPage),
  },
  {
    path: 'controls/odds',
    loadComponent: () =>
      import('./pages/controls/admin-baremes.page').then(m => m.AdminBaremesPage),
  },
  {
    path: 'controls/bonuses',
    loadComponent: () =>
      import('./pages/admin-placeholder.page').then(m => m.AdminPlaceholderPage),
    data: { titleKey: 'nav.admin.bonuses', icon: 'payments' },
  },
  {
    path: 'pricing',
    loadComponent: () =>
      import('./pages/pricing/admin-pricing.page').then(m => m.AdminPricingPage),
  },
  {
    path: 'limits',
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
    path: 'promotions',
    loadComponent: () =>
      import('./pages/admin-placeholder.page').then(m => m.AdminPlaceholderPage),
    data: { titleKey: 'nav.promotions', icon: 'local_activity' },
  },
  {
    path: 'promotions/maryaj-gratis',
    loadComponent: () =>
      import('./pages/admin-placeholder.page').then(m => m.AdminPlaceholderPage),
    data: { titleKey: 'nav.admin.maryaj_free', icon: 'redeem' },
  },
  {
    path: 'promotions/active',
    loadComponent: () =>
      import('./pages/admin-placeholder.page').then(m => m.AdminPlaceholderPage),
    data: { titleKey: 'nav.admin.active_promotions', icon: 'campaign' },
  },
  {
    path: 'reports',
    loadComponent: () =>
      import('./pages/reports/admin-today-report.page').then(m => m.AdminTodayReportPage),
  },
  {
    path: 'reports/today',
    loadComponent: () =>
      import('./pages/reports/admin-today-report.page').then(m => m.AdminTodayReportPage),
  },
  {
    path: 'reports/export',
    loadComponent: () =>
      import('./pages/admin-placeholder.page').then(m => m.AdminPlaceholderPage),
    data: { titleKey: 'nav.admin.export_print', icon: 'print' },
  },
  {
    path: 'support/tickets',
    loadComponent: () =>
      import('./pages/support/admin-tickets.page').then(m => m.AdminTicketsPage),
  },
  {
    path: 'support/sell',
    loadComponent: () =>
      import('./pages/support/admin-sell-ticket.page').then(m => m.AdminSellTicketPage),
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
  {
    path: 'i18n',
    loadComponent: () =>
      import('./pages/admin-placeholder.page').then(m => m.AdminPlaceholderPage),
    data: { titleKey: 'nav.translations', icon: 'translate' },
  },
  {
    path: 'appearance',
    loadComponent: () =>
      import('./pages/settings/admin-settings.page').then(m => m.AdminSettingsPage),
  },
  {
    path: 'pagemodels',
    loadComponent: () =>
      import('./pages/admin-placeholder.page').then(m => m.AdminPlaceholderPage),
    data: { titleKey: 'nav.pagemodels', icon: 'dashboard_customize' },
  },
  {
    path: 'more',
    loadComponent: () =>
      import('./pages/settings/admin-settings.page').then(m => m.AdminSettingsPage),
  },
  {
    path: 'more/space',
    loadComponent: () =>
      import('./pages/onboarding/admin-onboarding.page').then(m => m.AdminOnboardingPage),
  },
  {
    path: 'more/account',
    loadComponent: () => import('./pages/users/admin-users.page').then(m => m.AdminUsersPage),
  },
  {
    path: 'more/support',
    loadComponent: () =>
      import('./pages/support/admin-tickets.page').then(m => m.AdminTicketsPage),
  },
];
