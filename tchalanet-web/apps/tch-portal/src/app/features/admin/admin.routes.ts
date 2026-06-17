import { Route } from '@angular/router';

const placeholder = () =>
  import('./pages/admin-placeholder.page').then(m => m.AdminPlaceholderPage);

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
    path: 'sellers/new',
    loadComponent: () =>
      import('./pages/sellers/admin-seller-new.page').then(m => m.AdminSellerNewPage),
  },
  {
    path: 'sellers/:id',
    loadComponent: () =>
      import('./pages/sellers/admin-seller-detail.page').then(m => m.AdminSellerDetailPage),
  },
  {
    path: 'draws',
    loadComponent: () =>
      import('./pages/draws/admin-draws.page').then(m => m.AdminDrawsPage),
  },
  {
    path: 'draws/config/channels',
    loadComponent: () =>
      import('./pages/draws/admin-draw-channels.page').then(m => m.AdminDrawChannelsPage),
  },
  {
    path: 'draws/:id',
    loadComponent: () =>
      import('./pages/draws/admin-draw-detail.page').then(m => m.AdminDrawDetailPage),
  },
  {
    path: 'controls',
    loadComponent: placeholder,
    data: { titleKey: 'admin.controls.title', icon: 'tune' },
  },
  {
    path: 'controls/commission',
    loadComponent: () =>
      import('./pages/commission/admin-commission.page').then(m => m.AdminCommissionPage),
  },
  {
    path: 'controls/limits',
    loadComponent: placeholder,
    data: { titleKey: 'admin.controls.limits.title', icon: 'shield' },
  },
  {
    path: 'controls/baremes',
    loadComponent: () =>
      import('./pages/controls/admin-baremes.page').then(m => m.AdminBaremesPage),
  },
  {
    path: 'controls/bonuses',
    loadComponent: placeholder,
    data: { titleKey: 'admin.controls.bonuses.title', icon: 'payments' },
  },
  {
    path: 'promotions',
    loadComponent: placeholder,
    data: { titleKey: 'admin.promotions.title', icon: 'local_activity' },
  },
  {
    path: 'promotions/maryaj-gratis',
    loadComponent: placeholder,
    data: { titleKey: 'admin.promotions.maryaj_gratis.title', icon: 'redeem' },
  },
  {
    path: 'promotions/active',
    loadComponent: placeholder,
    data: { titleKey: 'admin.promotions.active.title', icon: 'campaign' },
  },
  {
    path: 'reports',
    loadComponent: placeholder,
    data: { titleKey: 'admin.reports.title', icon: 'analytics' },
  },
  {
    path: 'reports/today',
    loadComponent: () =>
      import('./pages/reports/admin-today-report.page').then(m => m.AdminTodayReportPage),
  },
  {
    path: 'reports/export',
    loadComponent: placeholder,
    data: { titleKey: 'admin.reports.export.title', icon: 'print' },
  },
  {
    path: 'more',
    loadComponent: placeholder,
    data: { titleKey: 'admin.more.title', icon: 'more_horiz' },
  },
  {
    path: 'more/space',
    loadComponent: placeholder,
    data: { titleKey: 'admin.more.space.title', icon: 'domain' },
  },
  {
    path: 'more/account',
    loadComponent: placeholder,
    data: { titleKey: 'admin.more.account.title', icon: 'account_circle' },
  },
  {
    path: 'more/support',
    loadComponent: placeholder,
    data: { titleKey: 'admin.more.support.title', icon: 'support_agent' },
  },
  {
    path: 'settings',
    loadComponent: () =>
      import('./pages/settings/admin-settings.page').then(m => m.AdminSettingsPage),
  },
  // Legacy routes kept during seller_terminal migration
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
];
