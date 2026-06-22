import { NavigationSection } from '@tch/api';

export type PrivateSpace = 'platform' | 'admin' | 'cashier';

export const PLATFORM_NAVIGATION: readonly NavigationSection[] = [
  {
    id: 'platform',
    titleKey: 'platform.nav.platform',
    items: [
      {
        id: 'overview',
        labelKey: 'platform.nav.overview',
        icon: 'space_dashboard',
        children: [
          {
            id: 'platform-dashboard',
            labelKey: 'platform.nav.dashboard',
            icon: 'dashboard',
            destination: { kind: 'route', value: '/app/platform/dashboard' },
            activeMatch: 'exact',
          },
          {
            id: 'platform-health',
            labelKey: 'platform.nav.health',
            icon: 'monitor_heart',
            destination: { kind: 'route', value: '/app/platform/health' },
          },
        ],
      },
      {
        id: 'tenants',
        labelKey: 'platform.nav.tenantsGroup',
        icon: 'business',
        children: [
          {
            id: 'tenant-list',
            labelKey: 'platform.nav.tenants',
            icon: 'domain',
            destination: { kind: 'route', value: '/app/platform/tenants' },
            activeMatch: 'exact',
          },
          {
            id: 'tenant-onboarding',
            labelKey: 'platform.nav.tenantOnboarding',
            icon: 'playlist_add_check',
            destination: { kind: 'route', value: '/app/platform/tenants/onboarding' },
          },
          {
            id: 'tenant-admins',
            labelKey: 'platform.nav.tenantAdmins',
            icon: 'admin_panel_settings',
            destination: { kind: 'route', value: '/app/platform/tenant-admins' },
          },
          {
            id: 'tenant-subscriptions',
            labelKey: 'platform.nav.subscriptions',
            icon: 'workspace_premium',
            destination: { kind: 'route', value: '/app/platform/subscriptions' },
          },
          {
            id: 'tenant-entitlements',
            labelKey: 'platform.nav.usageRights',
            icon: 'verified_user',
            destination: { kind: 'route', value: '/app/platform/entitlements' },
          },
        ],
      },
      {
        id: 'references',
        labelKey: 'platform.nav.references',
        icon: 'category',
        children: [
          {
            id: 'catalog-games',
            labelKey: 'platform.nav.games',
            icon: 'casino',
            destination: { kind: 'route', value: '/app/platform/catalog/games' },
          },
          {
            id: 'catalog-drawchannels',
            labelKey: 'platform.nav.drawChannels',
            icon: 'calendar_month',
            destination: { kind: 'route', value: '/app/platform/catalog/draw-channels' },
          },
          {
            id: 'catalog-resultslots',
            labelKey: 'platform.nav.resultSlots',
            icon: 'view_timeline',
            destination: { kind: 'route', value: '/app/platform/catalog/result-slots' },
          },
          {
            id: 'catalog-plans-pricing',
            labelKey: 'platform.nav.plansPricing',
            icon: 'payments',
            destination: { kind: 'route', value: '/app/platform/catalog/plans-pricing' },
          },
          {
            id: 'catalog-settings',
            labelKey: 'platform.nav.globalSettings',
            icon: 'settings',
            destination: { kind: 'route', value: '/app/platform/catalog/settings' },
          },
          {
            id: 'catalog-themes',
            labelKey: 'platform.nav.themes',
            icon: 'palette',
            destination: { kind: 'route', value: '/app/platform/catalog/themes' },
          },
          {
            id: 'catalog-translations',
            labelKey: 'platform.nav.translations',
            icon: 'translate',
            destination: { kind: 'route', value: '/app/platform/catalog/translations' },
          },
          {
            id: 'catalog-pagemodel-templates',
            labelKey: 'platform.nav.pageModelTemplates',
            icon: 'dashboard_customize',
            destination: { kind: 'route', value: '/app/platform/catalog/page-model-templates' },
          },
        ],
      },
      {
        id: 'operations',
        labelKey: 'platform.nav.operations',
        icon: 'manufacturing',
        children: [
          {
            id: 'ops-draw-results',
            labelKey: 'platform.nav.drawResults',
            icon: 'fact_check',
            destination: { kind: 'route', value: '/app/platform/ops/draw-results' },
          },
          {
            id: 'ops-providers',
            labelKey: 'platform.nav.providers',
            icon: 'cloud_sync',
            destination: { kind: 'route', value: '/app/platform/ops/providers' },
          },
          {
            id: 'ops-schedulers',
            labelKey: 'platform.nav.schedulers',
            icon: 'schedule',
            destination: { kind: 'route', value: '/app/platform/ops/schedulers' },
          },
          {
            id: 'ops-cache',
            labelKey: 'platform.nav.cache',
            icon: 'cached',
            destination: { kind: 'route', value: '/app/platform/ops/cache' },
          },
          {
            id: 'ops-archives',
            labelKey: 'platform.nav.archives',
            icon: 'inventory_2',
            destination: { kind: 'route', value: '/app/platform/ops/archives' },
          },
          {
            id: 'ops-audit',
            labelKey: 'platform.nav.audit',
            icon: 'assignment_turned_in',
            destination: { kind: 'route', value: '/app/platform/ops/audit' },
          },
        ],
      },
      {
        id: 'access',
        labelKey: 'platform.nav.accessRights',
        icon: 'shield',
        children: [
          {
            id: 'access-permissions',
            labelKey: 'platform.nav.permissions',
            icon: 'key',
            destination: { kind: 'route', value: '/app/platform/access/permissions' },
          },
          {
            id: 'access-roles',
            labelKey: 'platform.nav.roles',
            icon: 'groups',
            destination: { kind: 'route', value: '/app/platform/access/roles' },
          },
          {
            id: 'access-super-admins',
            labelKey: 'platform.nav.superAdmins',
            icon: 'admin_panel_settings',
            destination: { kind: 'route', value: '/app/platform/access/super-admins' },
          },
          {
            id: 'access-overrides',
            labelKey: 'platform.nav.superAdminOverrides',
            icon: 'security',
            destination: { kind: 'route', value: '/app/platform/access/overrides' },
          },
        ],
      },
      {
        id: 'communication',
        labelKey: 'platform.nav.communication',
        icon: 'campaign',
        children: [
          {
            id: 'communication-notifications',
            labelKey: 'platform.nav.inAppNotifications',
            icon: 'notifications',
            destination: { kind: 'route', value: '/app/platform/communication/notifications' },
          },
          {
            id: 'communication-contacts',
            labelKey: 'platform.nav.contactManagement',
            icon: 'contact_support',
            destination: { kind: 'route', value: '/app/platform/communication/contacts' },
          },
          {
            id: 'communication-news',
            labelKey: 'platform.nav.news',
            icon: 'article',
            destination: { kind: 'route', value: '/app/platform/communication/news' },
          },
        ],
      },
      {
        id: 'reports',
        labelKey: 'platform.nav.reports',
        icon: 'bar_chart',
        children: [
          {
            id: 'platform-reports',
            labelKey: 'platform.nav.platformReports',
            icon: 'analytics',
            destination: { kind: 'route', value: '/app/platform/reports' },
          },
        ],
      },
    ],
  },
];

export const TENANT_ADMIN_NAVIGATION: readonly NavigationSection[] = [
  {
    id: 'admin',
    titleKey: 'nav.admin.section.admin',
    items: [
      {
        id: 'dashboard',
        labelKey: 'nav.dashboard',
        icon: 'dashboard',
        destination: { kind: 'route', value: '/app/admin' },
        activeMatch: 'exact',
      },
      {
        id: 'sellerTerminals',
        labelKey: 'nav.admin.seller_terminals',
        icon: 'point_of_sale',
        destination: { kind: 'route', value: '/app/admin/seller-terminals' },
        children: [
          {
            id: 'sellerTerminalAdd',
            labelKey: 'nav.admin.seller_terminal_add',
            icon: 'add_circle',
            destination: { kind: 'route', value: '/app/admin/seller-terminals' },
          },
          {
            id: 'sellerTerminalActive',
            labelKey: 'nav.admin.seller_terminal_active',
            icon: 'check_circle',
            destination: { kind: 'route', value: '/app/admin/seller-terminals?status=active' },
          },
        ],
      },
      {
        id: 'draws',
        labelKey: 'nav.draws',
        icon: 'event',
        destination: { kind: 'route', value: '/app/admin/draws' },
        children: [
          {
            id: 'allDraws',
            labelKey: 'nav.admin.all_draws',
            icon: 'list',
            destination: { kind: 'route', value: '/app/admin/draws' },
            activeMatch: 'exact',
          },
          {
            id: 'upcomingDraws',
            labelKey: 'nav.admin.upcoming_draws',
            icon: 'event_upcoming',
            destination: { kind: 'route', value: '/app/admin/draws?status=upcoming' },
          },
          {
            id: 'pastDraws',
            labelKey: 'nav.admin.past_draws',
            icon: 'history',
            destination: { kind: 'route', value: '/app/admin/draws?status=past' },
          },
          {
            id: 'drawChannels',
            labelKey: 'nav.admin.draw_channels',
            icon: 'settings',
            destination: { kind: 'route', value: '/app/admin/draws/config/channels' },
          },
        ],
      },
      {
        id: 'controls',
        labelKey: 'nav.admin.controls',
        icon: 'tune',
        destination: { kind: 'route', value: '/app/admin/controls' },
        children: [
          {
            id: 'commission',
            labelKey: 'nav.admin.commission',
            icon: 'percent',
            destination: { kind: 'route', value: '/app/admin/controls/commission' },
          },
          {
            id: 'limits',
            labelKey: 'nav.limits',
            icon: 'shield',
            destination: { kind: 'route', value: '/app/admin/controls/limits' },
          },
          {
            id: 'baremes',
            labelKey: 'nav.admin.baremes',
            icon: 'format_list_numbered',
            destination: { kind: 'route', value: '/app/admin/controls/baremes' },
          },
          {
            id: 'bonuses',
            labelKey: 'nav.admin.bonuses',
            icon: 'payments',
            destination: { kind: 'route', value: '/app/admin/controls/bonuses' },
          },
        ],
      },
      {
        id: 'promotions',
        labelKey: 'nav.promotions',
        icon: 'local_activity',
        destination: { kind: 'route', value: '/app/admin/promotions' },
        children: [
          {
            id: 'maryajFree',
            labelKey: 'nav.admin.maryaj_free',
            icon: 'redeem',
            destination: { kind: 'route', value: '/app/admin/promotions/maryaj-gratis' },
          },
          {
            id: 'activePromotions',
            labelKey: 'nav.admin.active_promotions',
            icon: 'campaign',
            destination: { kind: 'route', value: '/app/admin/promotions/active' },
          },
        ],
      },
      {
        id: 'reports',
        labelKey: 'nav.reports',
        icon: 'analytics',
        destination: { kind: 'route', value: '/app/admin/reports' },
        children: [
          {
            id: 'todayReport',
            labelKey: 'nav.admin.today_report',
            icon: 'today',
            destination: { kind: 'route', value: '/app/admin/reports/today' },
          },
          {
            id: 'exportPrint',
            labelKey: 'nav.admin.export_print',
            icon: 'print',
            destination: { kind: 'route', value: '/app/admin/reports/export' },
          },
        ],
      },
      {
        id: 'more',
        labelKey: 'nav.admin.more',
        icon: 'more_horiz',
        destination: { kind: 'route', value: '/app/admin/more' },
        children: [
          {
            id: 'generalConfiguration',
            labelKey: 'nav.admin.general_configuration',
            icon: 'settings',
            destination: { kind: 'route', value: '/app/admin/settings' },
          },
          {
            id: 'mySpace',
            labelKey: 'nav.admin.my_space',
            icon: 'domain',
            destination: { kind: 'route', value: '/app/admin/more/space' },
          },
          {
            id: 'myAccount',
            labelKey: 'nav.admin.my_account',
            icon: 'account_circle',
            destination: { kind: 'route', value: '/app/admin/more/account' },
          },
          {
            id: 'support',
            labelKey: 'nav.admin.support',
            icon: 'support_agent',
            destination: { kind: 'route', value: '/app/admin/more/support' },
          },
        ],
      },
    ],
  },
];

export const CASHIER_NAVIGATION: readonly NavigationSection[] = [
  {
    id: 'cashier',
    titleKey: 'nav.cashier.section.cashier',
    items: [
      {
        id: 'cashier-dashboard',
        labelKey: 'nav.cashier.dashboard',
        icon: 'dashboard',
        destination: { kind: 'route', value: '/app/cashier' },
        activeMatch: 'exact',
      },
      {
        id: 'cashier-sell',
        labelKey: 'nav.cashier.sell',
        icon: 'point_of_sale',
        destination: { kind: 'route', value: '/app/cashier/sell' },
      },
    ],
  },
];
