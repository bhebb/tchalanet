import { Route } from '@angular/router';
import { PlaceholderPage } from '@tch/ui/components';

export const adminRoutes: Route[] = [
  // ── Accueil ────────────────────────────────────────────────────────────────
  {
    path: '',
    loadComponent: () =>
      import('./dashboard/admin-dashboard.page').then(m => m.AdminDashboardPage),
    data: { titleKey: 'nav.dashboard', icon: 'dashboard' },
  },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./dashboard/admin-dashboard.page').then(m => m.AdminDashboardPage),
    data: { titleKey: 'nav.dashboard', icon: 'dashboard' },
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
    path: 'seller-terminals',
    loadChildren: () =>
      import('./seller-terminals/admin-seller-terminals.routes').then(
        m => m.adminSellerTerminalsRoutes,
      ),
  },
  {
    path: 'pos',
    loadChildren: () => import('./pos/pos.routes').then(m => m.posRoutes),
  },
  // ── Tirages ────────────────────────────────────────────────────────────────
  {
    path: 'draws',
    loadChildren: () =>
      import('./draws/admin-generated-draws.routes').then(m => m.adminGeneratedDrawsRoutes),
  },
  { path: 'draw-sales-matrix', redirectTo: 'games/channel-matrix', pathMatch: 'full' },
  {
    path: 'draw-channels',
    loadChildren: () =>
      import('./draw-channels/admin-draw-channels.routes').then(m => m.adminDrawChannelsRoutes),
  },
  // ── Jeux disponibles ─────────────────────────────────────────────────────
  {
    path: 'games',
    loadChildren: () =>
      import('./games-pricing/admin-games-pricing.routes').then(m => m.adminGamesPricingRoutes),
  },
  // ── Limites ────────────────────────────────────────────────────────────────
  {
    path: 'limits',
    loadChildren: () =>
      import('./limits/admin-limits.routes').then(m => m.adminLimitsRoutes),
  },
  { path: 'controls/limits', redirectTo: 'limits', pathMatch: 'full' },
  // ── Contrôles de vente ─────────────────────────────────────────────────────
  { path: 'controls/games', redirectTo: 'games', pathMatch: 'full' },
  {
    path: 'controls/gains',
    loadComponent: () =>
      import('./controls/admin-baremes.page').then(m => m.AdminBaremesPage),
  },
  {
    path: 'controls/commissions',
    loadComponent: () =>
      import('./seller-configuration/admin-seller-configuration.page').then(
        m => m.AdminSellerConfigurationPage,
      ),
  },
  // Legacy control paths → new canonical paths
  { path: 'controls/baremes', redirectTo: 'controls/gains', pathMatch: 'full' },
  { path: 'controls/odds', redirectTo: 'controls/gains', pathMatch: 'full' },
  { path: 'controls/pricing-rules', redirectTo: 'controls/gains', pathMatch: 'full' },
  { path: 'controls/commission', redirectTo: 'controls/commissions', pathMatch: 'full' },
  { path: 'controls/bonuses', redirectTo: 'controls/commissions', pathMatch: 'full' },
  { path: 'commission', redirectTo: 'controls/commissions', pathMatch: 'full' },
  { path: 'controls', redirectTo: 'controls/commissions', pathMatch: 'full' },
  // ── Promotions ─────────────────────────────────────────────────────────────
  {
    path: 'maryaj-gratis',
    loadComponent: () =>
      import('./promotions/pages/maryaj-gratis/admin-maryaj-gratis.page').then(
        m => m.AdminMaryajGratisPage,
      ),
  },
  {
    path: 'promotions/maryaj-gratis',
    redirectTo: 'maryaj-gratis',
    pathMatch: 'full',
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
    path: 'reports/overview',
    loadComponent: () =>
      import('./reports/pages/report/report.page').then(m => m.AdminReportPage),
  },
  {
    path: 'reports/daily',
    loadComponent: () =>
      import('./reports/pages/daily/report-daily.page').then(m => m.AdminReportDailyPage),
  },
  {
    path: 'reports/sales',
    loadComponent: () =>
      import('./reports/admin-today-report.page').then(m => m.AdminTodayReportPage),
  },
  {
    path: 'reports/sellers',
    loadComponent: () =>
      import('./reports/pages/sellers/report-sellers.page').then(m => m.AdminReportSellersPage),
  },
  {
    path: 'reports/draws',
    loadComponent: () =>
      import('./reports/pages/draws/report-draws.page').then(m => m.AdminReportDrawsPage),
  },
  {
    path: 'reports/financials',
    loadComponent: () =>
      import('./reports/pages/financials/admin-financials.page').then(m => m.AdminFinancialsPage),
  },
  { path: 'reports', redirectTo: 'reports/overview', pathMatch: 'full' },
  { path: 'reports/today', redirectTo: 'reports/daily', pathMatch: 'full' },
  { path: 'reports/export', redirectTo: 'reports/daily', pathMatch: 'full' },
  // ── Tickets ────────────────────────────────────────────────────────────────
  {
    path: 'tickets/overview',
    loadComponent: () =>
      import('./sales-admin/pages/admin-tickets-overview/admin-tickets-overview.page').then(
        m => m.AdminTicketsOverviewPage,
      ),
  },
  {
    path: 'tickets/verify',
    loadComponent: () =>
      import('./pos/sale/pages/verify/pos-ticket-verify.page').then(m => m.PosTicketVerifyPage),
    data: { titleKey: 'nav.admin.tickets_verify', icon: 'verified' },
  },
  {
    path: 'tickets/sell',
    redirectTo: '/app/admin/pos/sale',
    pathMatch: 'full',
  },
  {
    path: 'tickets/:ticketId',
    loadComponent: () =>
      import('./pos/sale/pages/ticket-detail/pos-ticket-detail.page').then(
        m => m.PosTicketDetailPage,
      ),
  },
  {
    path: 'tickets',
    loadComponent: () =>
      import('./sales-admin/pages/admin-tickets/admin-tickets.page').then(m => m.AdminTicketsPage),
  },
  { path: 'support/tickets', redirectTo: 'tickets', pathMatch: 'full' },
  { path: 'support/sell', redirectTo: 'tickets/sell', pathMatch: 'full' },
  // ── Mon entreprise ─────────────────────────────────────────────────────────
  {
    path: 'company/appearance',
    loadComponent: () =>
      import('./appearance/admin-appearance.page').then(m => m.AdminAppearancePage),
    data: { titleKey: 'nav.admin.company_appearance', icon: 'palette' },
  },
  {
    path: 'company/settings',
    loadComponent: () =>
      import('./setup/pages/settings/admin-settings.page').then(m => m.AdminSettingsPage),
  },
  {
    path: 'company/settings/runtime',
    loadComponent: () =>
      import('./setup/pages/settings/admin-runtime.page').then(m => m.AdminRuntimePage),
  },
  {
    path: 'company/settings/config',
    loadComponent: () =>
      import('./setup/pages/settings/admin-config.page').then(m => m.AdminConfigPage),
  },
  {
    path: 'company/support',
    loadComponent: () =>
      import('./support/pages/admin-support.page').then(m => m.AdminSupportPage),
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
    component: PlaceholderPage,
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
    loadComponent: () => import('./admin-users/admin-users.page').then(m => m.AdminUsersPage),
  },
  { path: 'games-pricing', redirectTo: 'games', pathMatch: 'full' },
  {
    path: 'business-days',
    loadComponent: () =>
      import('./business-days/admin-business-days.page').then(m => m.AdminBusinessDaysPage),
  },
  {
    path: 'pricing',
    loadComponent: () =>
      import('./pricing/admin-pricing.page').then(m => m.AdminPricingPage),
  },
  {
    path: 'draw-results',
    redirectTo: 'draws/results',
    pathMatch: 'full',
  },
  {
    path: 'subscription',
    loadComponent: () =>
      import('./subscription/admin-subscription.page').then(m => m.AdminSubscriptionPage),
  },
  {
    path: 'settings',
    redirectTo: 'company/settings',
    pathMatch: 'full',
  },
  {
    path: 'settings/runtime',
    redirectTo: 'company/settings/runtime',
    pathMatch: 'full',
  },
  {
    path: 'settings/config',
    redirectTo: 'company/settings/config',
    pathMatch: 'full',
  },
  { path: 'appearance', redirectTo: 'company/appearance', pathMatch: 'full' },
  { path: 'more', redirectTo: 'company/settings', pathMatch: 'full' },
  { path: 'more/space', redirectTo: 'setup', pathMatch: 'full' },
  { path: 'more/account', redirectTo: 'users', pathMatch: 'full' },
  { path: 'more/support', redirectTo: 'company/support', pathMatch: 'full' },
  {
    path: 'i18n',
    component: PlaceholderPage,
    data: { titleKey: 'nav.translations', icon: 'translate' },
  },
  {
    path: 'pagemodels',
    component: PlaceholderPage,
    data: { titleKey: 'nav.pagemodels', icon: 'dashboard_customize' },
  },
];
