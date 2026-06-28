import { Route } from '@angular/router';

export const adminRoutes: Route[] = [
  // ── Accueil ────────────────────────────────────────────────────────────────
  {
    path: '',
    loadComponent: () =>
      import('../shell/page-model-host/private-dashboard.page').then(m => m.PrivateDashboardPage),
  },
  // ── Configuration générale ─────────────────────────────────────────────────
  {
    path: 'setup',
    loadChildren: () =>
      import('./setup/admin-setup.routes').then(m => m.adminSetupRoutes),
  },
  { path: 'onboarding', redirectTo: 'setup', pathMatch: 'full' },
  { path: 'complete-config', redirectTo: 'setup', pathMatch: 'full' },
  // ── Vendeurs ───────────────────────────────────────────────────────────────
  {
    path: 'sellers',
    loadChildren: () =>
      import('./seller-terminals/admin-seller-terminals.routes').then(
        m => m.adminSellerTerminalsRoutes,
      ),
  },
  {
    path: 'seller-terminals',
    loadChildren: () =>
      import('./seller-terminals/admin-seller-terminals.routes').then(
        m => m.adminSellerTerminalsRoutes,
      ),
  },
  // ── Tirages ────────────────────────────────────────────────────────────────
  {
    path: 'draws',
    loadChildren: () =>
      import('./draws/admin-generated-draws.routes').then(m => m.adminGeneratedDrawsRoutes),
  },
  { path: 'draw-sales-matrix', redirectTo: 'draws/matrix', pathMatch: 'full' },
  {
    path: 'draw-channels',
    loadChildren: () =>
      import('./draw-channels/admin-draw-channels.routes').then(m => m.adminDrawChannelsRoutes),
  },
  // ── Limites ────────────────────────────────────────────────────────────────
  {
    path: 'limits',
    loadComponent: () =>
      import('./pages/limits/admin-limits.page').then(m => m.AdminLimitsPage),
  },
  {
    path: 'limits/rules',
    loadComponent: () =>
      import('./pages/limits/admin-limits-rules.page').then(m => m.AdminLimitsRulesPage),
  },
  { path: 'controls/limits', redirectTo: 'limits', pathMatch: 'full' },
  // ── Contrôles de vente ─────────────────────────────────────────────────────
  {
    path: 'controls/games',
    loadChildren: () =>
      import('./games-pricing/admin-games-pricing.routes').then(m => m.adminGamesPricingRoutes),
  },
  {
    path: 'controls/gains',
    loadComponent: () =>
      import('./pages/controls/admin-baremes.page').then(m => m.AdminBaremesPage),
  },
  {
    path: 'controls/commissions',
    loadComponent: () =>
      import('./pages/commission/admin-commission.page').then(m => m.AdminCommissionPage),
  },
  // Legacy control paths → new canonical paths
  { path: 'controls/baremes', redirectTo: 'controls/gains', pathMatch: 'full' },
  { path: 'controls/odds', redirectTo: 'controls/gains', pathMatch: 'full' },
  { path: 'controls/commission', redirectTo: 'controls/commissions', pathMatch: 'full' },
  { path: 'controls/bonuses', redirectTo: 'controls/commissions', pathMatch: 'full' },
  { path: 'commission', redirectTo: 'controls/commissions', pathMatch: 'full' },
  { path: 'controls', redirectTo: 'controls/commissions', pathMatch: 'full' },
  // ── Promotions ─────────────────────────────────────────────────────────────
  {
    path: 'promotions/maryaj-gratis',
    loadComponent: () =>
      import('./promotions/pages/maryaj-gratis/admin-maryaj-gratis.page').then(
        m => m.AdminMaryajGratisPage,
      ),
  },
  {
    path: 'promotions',
    loadComponent: () =>
      import('./promotions/pages/campaigns/admin-promotion-campaigns.page').then(
        m => m.AdminPromotionCampaignsPage,
      ),
  },
  { path: 'promotions/active', redirectTo: 'promotions', pathMatch: 'full' },
  // ── Rapports ───────────────────────────────────────────────────────────────
  {
    path: 'reports/sales',
    loadComponent: () =>
      import('./pages/reports/admin-today-report.page').then(m => m.AdminTodayReportPage),
  },
  {
    path: 'reports/sellers',
    loadComponent: () =>
      import('./pages/admin-placeholder.page').then(m => m.AdminPlaceholderPage),
    data: { titleKey: 'nav.admin.reports_sellers', icon: 'people' },
  },
  {
    path: 'reports/draws',
    loadComponent: () =>
      import('./pages/admin-placeholder.page').then(m => m.AdminPlaceholderPage),
    data: { titleKey: 'nav.admin.reports_draws', icon: 'event' },
  },
  {
    path: 'reports/exports',
    loadComponent: () =>
      import('./pages/admin-placeholder.page').then(m => m.AdminPlaceholderPage),
    data: { titleKey: 'nav.admin.reports_exports', icon: 'download' },
  },
  {
    path: 'reports/financials',
    loadComponent: () =>
      import('./financials/pages/admin-financials.page').then(m => m.AdminFinancialsPage),
  },
  { path: 'reports', redirectTo: 'reports/sales', pathMatch: 'full' },
  { path: 'reports/today', redirectTo: 'reports/sales', pathMatch: 'full' },
  { path: 'reports/export', redirectTo: 'reports/exports', pathMatch: 'full' },
  // ── Tickets ────────────────────────────────────────────────────────────────
  {
    path: 'tickets',
    loadComponent: () =>
      import('./pages/support/admin-tickets.page').then(m => m.AdminTicketsPage),
  },
  {
    path: 'tickets/sell/:sellerTerminalId',
    loadComponent: () =>
      import('./seller-terminals/pages/pos/admin-seller-terminal-pos.page').then(
        m => m.AdminSellerTerminalPosPage,
      ),
  },
  {
    path: 'tickets/sell',
    loadComponent: () =>
      import('./seller-terminals/pages/sell-entry/admin-sell-entry.page').then(
        m => m.AdminSellEntryPage,
      ),
  },
  {
    path: 'tickets/verify',
    loadComponent: () =>
      import('./pages/admin-placeholder.page').then(m => m.AdminPlaceholderPage),
    data: { titleKey: 'nav.admin.tickets_verify', icon: 'verified' },
  },
  { path: 'support/tickets', redirectTo: 'tickets', pathMatch: 'full' },
  { path: 'support/sell', redirectTo: 'tickets/sell', pathMatch: 'full' },
  // ── Mon entreprise ─────────────────────────────────────────────────────────
  {
    path: 'company/identity',
    loadComponent: () =>
      import('./pages/admin-placeholder.page').then(m => m.AdminPlaceholderPage),
    data: { titleKey: 'nav.admin.company_identity', icon: 'domain' },
  },
  {
    path: 'company/address',
    loadComponent: () =>
      import('./pages/admin-placeholder.page').then(m => m.AdminPlaceholderPage),
    data: { titleKey: 'nav.admin.company_address', icon: 'location_on' },
  },
  {
    path: 'company/appearance',
    loadComponent: () =>
      import('./pages/admin-placeholder.page').then(m => m.AdminPlaceholderPage),
    data: { titleKey: 'nav.admin.company_appearance', icon: 'palette' },
  },
  {
    path: 'company/settings',
    loadComponent: () =>
      import('./pages/admin-placeholder.page').then(m => m.AdminPlaceholderPage),
    data: { titleKey: 'nav.admin.company_settings', icon: 'tune' },
  },
  {
    path: 'company/support',
    loadComponent: () =>
      import('./pages/admin-placeholder.page').then(m => m.AdminPlaceholderPage),
    data: { titleKey: 'nav.admin.company_support', icon: 'headset_mic' },
  },
  {
    path: 'notifications',
    loadComponent: () =>
      import('./notifications/admin-notifications.page').then(m => m.AdminNotificationsPage),
    data: { titleKey: 'nav.admin.company_notifications', icon: 'notifications' },
  },
  // ── Aide ───────────────────────────────────────────────────────────────────
  {
    path: 'help',
    loadComponent: () =>
      import('./pages/admin-placeholder.page').then(m => m.AdminPlaceholderPage),
    data: { titleKey: 'nav.admin.help', icon: 'help_outline' },
  },
  // ── Legacy paths (preserved for deep-links and bookmarks) ──────────────────
  {
    path: 'business-profile',
    loadChildren: () =>
      import('./business-profile/admin-business-profile.routes').then(
        m => m.adminBusinessProfileRoutes,
      ),
  },
  {
    path: 'users',
    loadComponent: () => import('./pages/users/admin-users.page').then(m => m.AdminUsersPage),
  },
  {
    path: 'games',
    loadComponent: () =>
      import('./pages/games/admin-games.page').then(m => m.AdminGamesPage),
  },
  {
    path: 'games-pricing',
    loadChildren: () =>
      import('./games-pricing/admin-games-pricing.routes').then(m => m.adminGamesPricingRoutes),
  },
  {
    path: 'business-days',
    loadComponent: () =>
      import('./pages/business-days/admin-business-days.page').then(m => m.AdminBusinessDaysPage),
  },
  {
    path: 'pricing',
    loadComponent: () =>
      import('./pages/pricing/admin-pricing.page').then(m => m.AdminPricingPage),
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
      import('./setup/pages/settings/admin-settings.page').then(m => m.AdminSettingsPage),
  },
  {
    path: 'settings/runtime',
    loadComponent: () =>
      import('./setup/pages/settings/admin-runtime.page').then(m => m.AdminRuntimePage),
  },
  {
    path: 'settings/config',
    loadComponent: () =>
      import('./setup/pages/settings/admin-config.page').then(m => m.AdminConfigPage),
  },
  { path: 'appearance', redirectTo: 'company/appearance', pathMatch: 'full' },
  { path: 'more', redirectTo: 'company/settings', pathMatch: 'full' },
  { path: 'more/space', redirectTo: 'setup', pathMatch: 'full' },
  { path: 'more/account', redirectTo: 'users', pathMatch: 'full' },
  { path: 'more/support', redirectTo: 'company/support', pathMatch: 'full' },
  {
    path: 'i18n',
    loadComponent: () =>
      import('./pages/admin-placeholder.page').then(m => m.AdminPlaceholderPage),
    data: { titleKey: 'nav.translations', icon: 'translate' },
  },
  {
    path: 'pagemodels',
    loadComponent: () =>
      import('./pages/admin-placeholder.page').then(m => m.AdminPlaceholderPage),
    data: { titleKey: 'nav.pagemodels', icon: 'dashboard_customize' },
  },
];
