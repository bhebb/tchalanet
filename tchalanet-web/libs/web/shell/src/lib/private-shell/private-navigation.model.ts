import { NavigationSection } from '@tch/api';

export type PrivateSpace = 'platform' | 'admin' | 'cashier';

export const PLATFORM_NAVIGATION: readonly NavigationSection[] = [
  {
    id: 'platform',
    titleKey: 'platform.nav.platform',
    items: [
      {
        id: 'dashboard',
        labelKey: 'platform.nav.dashboard',
        icon: 'space_dashboard',
        children: [
          {
            id: 'platform-health',
            labelKey: 'platform.nav.opsDashboard',
            icon: 'monitor_heart',
            destination: { kind: 'route', value: '/app/platform' },
            activeMatch: 'exact',
          },
          {
            id: 'platform-dashboard',
            labelKey: 'platform.nav.commercialDashboard',
            icon: 'dashboard',
            destination: { kind: 'route', value: '/app/platform/dashboard' },
            activeMatch: 'exact',
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
            id: 'tenant-support',
            labelKey: 'platform.nav.supportTenant',
            icon: 'support_agent',
            destination: { kind: 'route', value: '/app/platform/support-tenant' },
          },
        ],
      },
      {
        id: 'operations',
        labelKey: 'platform.nav.operations',
        icon: 'manufacturing',
        children: [
          {
            id: 'ops-overview',
            labelKey: 'platform.nav.overview',
            icon: 'monitor_heart',
            destination: { kind: 'route', value: '/app/platform/ops' },
            activeMatch: 'exact',
          },
          {
            id: 'ops-draws',
            labelKey: 'platform.nav.draws',
            icon: 'event',
            destination: { kind: 'route', value: '/app/platform/ops/draws' },
          },
          {
            id: 'ops-draw-results',
            labelKey: 'platform.nav.drawResults',
            icon: 'fact_check',
            destination: { kind: 'route', value: '/app/platform/ops/draw-results' },
          },
          {
            id: 'ops-jobs',
            labelKey: 'platform.nav.jobs',
            icon: 'schedule',
            destination: { kind: 'route', value: '/app/platform/ops/jobs' },
          },
          {
            id: 'ops-cache',
            labelKey: 'platform.nav.cache',
            icon: 'cached',
            destination: { kind: 'route', value: '/app/platform/ops/cache' },
          },
        ],
      },
      {
        id: 'access',
        labelKey: 'platform.nav.accessSecurity',
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
            id: 'access-user-access',
            labelKey: 'platform.nav.accessUsers',
            icon: 'manage_accounts',
            destination: { kind: 'route', value: '/app/platform/access/users' },
          },
          {
            id: 'access-super-admins',
            labelKey: 'platform.nav.superAdmins',
            icon: 'admin_panel_settings',
            destination: { kind: 'route', value: '/app/platform/super-admins' },
          },
          {
            id: 'access-admin-accounts',
            labelKey: 'platform.nav.users',
            icon: 'supervised_user_circle',
            destination: { kind: 'route', value: '/app/platform/tenant-admins' },
          },
          {
            id: 'access-backend-keys',
            labelKey: 'platform.nav.backendKeys',
            icon: 'vpn_key',
            destination: { kind: 'route', value: '/app/platform/access/backend-keys' },
          },
        ],
      },
      {
        id: 'audit',
        labelKey: 'platform.nav.audit',
        icon: 'assignment_turned_in',
        children: [
          {
            id: 'audit-functional',
            labelKey: 'platform.nav.functionalAudit',
            icon: 'assignment_turned_in',
            destination: { kind: 'route', value: '/app/platform/audit' },
            activeMatch: 'exact',
          },
          {
            id: 'audit-entity-history',
            labelKey: 'platform.nav.entityHistory',
            icon: 'manage_history',
            destination: { kind: 'route', value: '/app/platform/audit/entity-history' },
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
            id: 'catalog-draw-channels',
            labelKey: 'platform.nav.drawChannels',
            icon: 'calendar_month',
            destination: { kind: 'route', value: '/app/platform/catalog/draw-channels' },
          },
          {
            id: 'catalog-result-slots',
            labelKey: 'platform.nav.resultSlots',
            icon: 'view_timeline',
            destination: { kind: 'route', value: '/app/platform/catalog/result-slots' },
          },
          {
            id: 'catalog-pricing',
            labelKey: 'platform.nav.pricing',
            icon: 'payments',
            destination: { kind: 'route', value: '/app/platform/catalog/pricing' },
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
        ],
      },
      {
        id: 'archives',
        labelKey: 'platform.nav.archives',
        icon: 'inventory_2',
        children: [
          {
            id: 'archive-overview',
            labelKey: 'platform.nav.overview',
            icon: 'inventory_2',
            destination: { kind: 'route', value: '/app/platform/archives' },
            activeMatch: 'exact',
          },
          {
            id: 'archive-runs',
            labelKey: 'platform.nav.archiveRuns',
            icon: 'history',
            destination: { kind: 'route', value: '/app/platform/archives/runs' },
          },
          {
            id: 'archive-issues',
            labelKey: 'platform.nav.archiveIssues',
            icon: 'report',
            destination: { kind: 'route', value: '/app/platform/archives/issues' },
          },
          {
            id: 'archive-legal-holds',
            labelKey: 'platform.nav.legalHolds',
            icon: 'gavel',
            destination: { kind: 'route', value: '/app/platform/archives/legal-holds' },
          },
          {
            id: 'archive-partitions',
            labelKey: 'platform.nav.partitions',
            icon: 'table_rows',
            destination: { kind: 'route', value: '/app/platform/archives/partitions' },
          },
          {
            id: 'archive-purges',
            labelKey: 'platform.nav.purges',
            icon: 'delete_sweep',
            destination: { kind: 'route', value: '/app/platform/archives/purges' },
          },
        ],
      },
      {
        id: 'support-and-content',
        labelKey: 'platform.nav.communicationSupport',
        icon: 'campaign',
        children: [
          {
            id: 'notifications',
            labelKey: 'platform.nav.inAppNotifications',
            icon: 'notifications',
            destination: { kind: 'route', value: '/app/platform/communication/notifications' },
          },
          {
            id: 'contact-requests',
            labelKey: 'platform.nav.contactRequests',
            icon: 'contact_support',
            destination: { kind: 'route', value: '/app/platform/communication/contacts' },
          },
          {
            id: 'news',
            labelKey: 'platform.nav.news',
            icon: 'article',
            destination: { kind: 'route', value: '/app/platform/communication/news' },
          },
          {
            id: 'contact-config',
            labelKey: 'platform.nav.contactConfig',
            icon: 'contact_mail',
            destination: { kind: 'route', value: '/app/platform/communication/config' },
          },
          {
            id: 'communication-outbox',
            labelKey: 'platform.nav.communicationOutbox',
            icon: 'outbox',
            destination: { kind: 'route', value: '/app/platform/communication/outbox' },
          },
          {
            id: 'communication-tests',
            labelKey: 'platform.nav.communicationTests',
            icon: 'send',
            destination: { kind: 'route', value: '/app/platform/communication/tests' },
          },
        ],
      },
      {
        id: 'tchala',
        labelKey: 'platform.nav.tchala',
        icon: 'auto_stories',
        children: [
          {
            id: 'tchala-suggestions',
            labelKey: 'platform.nav.tchalaSuggestions',
            icon: 'lightbulb',
            destination: { kind: 'route', value: '/app/platform/tchala/suggestions' },
          },
          {
            id: 'tchala-import',
            labelKey: 'platform.nav.tchalaImport',
            icon: 'upload_file',
            destination: { kind: 'route', value: '/app/platform/tchala/import' },
          },
          {
            id: 'tchala-cleanup',
            labelKey: 'platform.nav.tchalaCleanup',
            icon: 'auto_fix_high',
            destination: { kind: 'route', value: '/app/platform/tchala/cleanup' },
          },
        ],
      },
      {
        id: 'reports',
        labelKey: 'platform.nav.platformReports',
        icon: 'bar_chart',
        destination: { kind: 'route', value: '/app/platform/reports' },
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
        id: 'setup',
        labelKey: 'nav.admin.general_configuration',
        icon: 'checklist',
        destination: { kind: 'route', value: '/app/admin/setup' },
      },
      {
        id: 'sellers',
        labelKey: 'nav.admin.sellers',
        icon: 'point_of_sale',
        destination: { kind: 'route', value: '/app/admin/sellers' },
        children: [
          {
            id: 'sellers-list',
            labelKey: 'nav.admin.sellers_list',
            icon: 'list',
            destination: { kind: 'route', value: '/app/admin/sellers' },
            activeMatch: 'exact',
          },
          {
            id: 'sellers-new',
            labelKey: 'nav.admin.sellers_new',
            icon: 'add_circle',
            destination: { kind: 'route', value: '/app/admin/sellers/new' },
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
            id: 'draws-all',
            labelKey: 'nav.admin.all_draws',
            icon: 'list',
            destination: { kind: 'route', value: '/app/admin/draws' },
            activeMatch: 'exact',
          },
          {
            id: 'draws-open',
            labelKey: 'nav.admin.draws_open',
            icon: 'event_available',
            destination: { kind: 'route', value: '/app/admin/draws?status=open' },
          },
          {
            id: 'draws-past',
            labelKey: 'nav.admin.draws_past',
            icon: 'history',
            destination: { kind: 'route', value: '/app/admin/draws?status=past' },
          },
          {
            id: 'draws-matrix',
            labelKey: 'nav.admin.draws_matrix',
            icon: 'grid_view',
            destination: { kind: 'route', value: '/app/admin/draws/matrix' },
          },
          {
            id: 'draws-channels',
            labelKey: 'nav.admin.draws_channels',
            icon: 'settings',
            destination: { kind: 'route', value: '/app/admin/draws/channels' },
          },
        ],
      },
      {
        id: 'limits',
        labelKey: 'nav.admin.limits_section',
        icon: 'shield',
        destination: { kind: 'route', value: '/app/admin/limits' },
        children: [
          {
            id: 'limits-system',
            labelKey: 'nav.admin.limits_system',
            icon: 'lock',
            destination: { kind: 'route', value: '/app/admin/limits?scope=system' },
          },
          {
            id: 'limits-global',
            labelKey: 'nav.admin.limits_global',
            icon: 'tune',
            destination: { kind: 'route', value: '/app/admin/limits?scope=global' },
          },
          {
            id: 'limits-seller',
            labelKey: 'nav.admin.limits_seller',
            icon: 'person',
            destination: { kind: 'route', value: '/app/admin/limits?scope=seller' },
          },
          {
            id: 'limits-number',
            labelKey: 'nav.admin.limits_number',
            icon: 'pin',
            destination: { kind: 'route', value: '/app/admin/limits?scope=number' },
          },
          {
            id: 'limits-game',
            labelKey: 'nav.admin.limits_game',
            icon: 'casino',
            destination: { kind: 'route', value: '/app/admin/limits?scope=game' },
          },
          {
            id: 'limits-draw',
            labelKey: 'nav.admin.limits_draw',
            icon: 'event',
            destination: { kind: 'route', value: '/app/admin/limits?scope=draw' },
          },
        ],
      },
      {
        id: 'controls',
        labelKey: 'nav.admin.controls_sale',
        icon: 'tune',
        destination: { kind: 'route', value: '/app/admin/controls/games' },
        children: [
          {
            id: 'controls-games',
            labelKey: 'nav.admin.controls_games',
            icon: 'casino',
            destination: { kind: 'route', value: '/app/admin/controls/games' },
          },
          {
            id: 'controls-gains',
            labelKey: 'nav.admin.controls_gains',
            icon: 'format_list_numbered',
            destination: { kind: 'route', value: '/app/admin/controls/gains' },
          },
          {
            id: 'controls-commissions',
            labelKey: 'nav.admin.controls_commissions',
            icon: 'percent',
            destination: { kind: 'route', value: '/app/admin/controls/commissions' },
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
            id: 'promotions-maryaj',
            labelKey: 'nav.admin.maryaj_free',
            icon: 'redeem',
            destination: { kind: 'route', value: '/app/admin/promotions/maryaj-gratis' },
          },
          {
            id: 'promotions-others',
            labelKey: 'nav.admin.active_promotions',
            icon: 'campaign',
            destination: { kind: 'route', value: '/app/admin/promotions' },
            activeMatch: 'exact',
          },
        ],
      },
      {
        id: 'reports',
        labelKey: 'nav.reports',
        icon: 'analytics',
        destination: { kind: 'route', value: '/app/admin/reports/sales' },
        children: [
          {
            id: 'reports-sales',
            labelKey: 'nav.admin.reports_sales',
            icon: 'today',
            destination: { kind: 'route', value: '/app/admin/reports/sales' },
          },
          {
            id: 'reports-sellers',
            labelKey: 'nav.admin.reports_sellers',
            icon: 'people',
            destination: { kind: 'route', value: '/app/admin/reports/sellers' },
          },
          {
            id: 'reports-draws',
            labelKey: 'nav.admin.reports_draws',
            icon: 'event_note',
            destination: { kind: 'route', value: '/app/admin/reports/draws' },
          },
          {
            id: 'reports-financials',
            labelKey: 'nav.admin.reports_financials',
            icon: 'query_stats',
            destination: { kind: 'route', value: '/app/admin/reports/financials' },
          },
          {
            id: 'reports-exports',
            labelKey: 'nav.admin.reports_exports',
            icon: 'download',
            destination: { kind: 'route', value: '/app/admin/reports/exports' },
          },
        ],
      },
      {
        id: 'tickets',
        labelKey: 'nav.admin.tickets_section',
        icon: 'confirmation_number',
        destination: { kind: 'route', value: '/app/admin/tickets' },
        children: [
          {
            id: 'tickets-list',
            labelKey: 'nav.admin.tickets_list',
            icon: 'list',
            destination: { kind: 'route', value: '/app/admin/tickets' },
            activeMatch: 'exact',
          },
          {
            id: 'tickets-sell',
            labelKey: 'nav.admin.tickets_sell',
            icon: 'point_of_sale',
            destination: { kind: 'route', value: '/app/admin/tickets/sell' },
          },
          {
            id: 'tickets-verify',
            labelKey: 'nav.admin.tickets_verify',
            icon: 'verified',
            destination: { kind: 'route', value: '/app/admin/tickets/verify' },
          },
        ],
      },
      {
        id: 'company',
        labelKey: 'nav.admin.company',
        icon: 'business',
        destination: { kind: 'route', value: '/app/admin/company/identity' },
        children: [
          {
            id: 'company-identity',
            labelKey: 'nav.admin.company_identity',
            icon: 'domain',
            destination: { kind: 'route', value: '/app/admin/company/identity' },
          },
          {
            id: 'company-address',
            labelKey: 'nav.admin.company_address',
            icon: 'location_on',
            destination: { kind: 'route', value: '/app/admin/company/address' },
          },
          {
            id: 'company-appearance',
            labelKey: 'nav.admin.company_appearance',
            icon: 'palette',
            destination: { kind: 'route', value: '/app/admin/company/appearance' },
          },
          {
            id: 'company-settings',
            labelKey: 'nav.admin.company_settings',
            icon: 'settings',
            destination: { kind: 'route', value: '/app/admin/company/settings' },
          },
          {
            id: 'company-notifications',
            labelKey: 'nav.admin.company_notifications',
            icon: 'notifications',
            destination: { kind: 'route', value: '/app/admin/notifications' },
          },
          {
            id: 'company-support',
            labelKey: 'nav.admin.company_support',
            icon: 'headset_mic',
            destination: { kind: 'route', value: '/app/admin/company/support' },
          },
        ],
      },
      {
        id: 'help',
        labelKey: 'nav.admin.help',
        icon: 'help_outline',
        destination: { kind: 'route', value: '/app/admin/help' },
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
