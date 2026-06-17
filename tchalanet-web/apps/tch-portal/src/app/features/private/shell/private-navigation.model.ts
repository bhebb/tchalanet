import { NavigationSection } from '@tch/api';

export type PrivateSpace = 'platform' | 'admin' | 'cashier';

export const PLATFORM_NAVIGATION: readonly NavigationSection[] = [
  {
    id: 'platform',
    titleKey: 'nav.platform.section.platform',
    items: [
      {
        id: 'platform-dashboard',
        labelKey: 'nav.platform.dashboard',
        icon: 'dashboard',
        destination: { kind: 'route', value: '/app/platform' },
        activeMatch: 'exact',
      },
      {
        id: 'tenants',
        labelKey: 'nav.platform.tenants',
        icon: 'business',
        destination: { kind: 'route', value: '/app/platform/tenants' },
      },
      {
        id: 'tenant-provisioning',
        labelKey: 'nav.platform.tenantProvisioning',
        icon: 'rocket_launch',
        destination: { kind: 'route', value: '/app/platform/tenant-provisioning' },
      },
      {
        id: 'contact-requests',
        labelKey: 'nav.platform.contactRequests',
        icon: 'contact_support',
        destination: { kind: 'route', value: '/app/platform/contact-requests' },
      },
      {
        id: 'news',
        labelKey: 'nav.platform.news',
        icon: 'article',
        destination: { kind: 'route', value: '/app/platform/news' },
      },
      {
        id: 'notifications',
        labelKey: 'nav.platform.notifications',
        icon: 'mark_email_unread',
        destination: { kind: 'route', value: '/app/platform/notifications' },
      },
      {
        id: 'ops',
        labelKey: 'nav.platform.ops',
        icon: 'monitor_heart',
        destination: { kind: 'route', value: '/app/platform/ops' },
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
        id: 'sellers',
        labelKey: 'nav.admin.sellers',
        icon: 'point_of_sale',
        destination: { kind: 'route', value: '/app/admin/sellers' },
        children: [
          {
            id: 'sellerAdd',
            labelKey: 'nav.admin.seller_add',
            icon: 'person_add',
            destination: { kind: 'route', value: '/app/admin/sellers/new' },
          },
          {
            id: 'sellerActive',
            labelKey: 'nav.admin.seller_active',
            icon: 'check_circle',
            destination: { kind: 'route', value: '/app/admin/sellers?status=active' },
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
