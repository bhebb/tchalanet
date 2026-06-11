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
        id: 'admin-dashboard',
        labelKey: 'nav.admin.dashboard',
        icon: 'dashboard',
        destination: { kind: 'route', value: '/app/admin' },
        activeMatch: 'exact',
      },
      {
        id: 'onboarding',
        labelKey: 'nav.admin.onboarding',
        icon: 'checklist',
        destination: { kind: 'route', value: '/app/admin/onboarding' },
      },
      {
        id: 'users',
        labelKey: 'nav.admin.users',
        icon: 'group',
        destination: { kind: 'route', value: '/app/admin/users' },
      },
      {
        id: 'sellers',
        labelKey: 'nav.admin.sellers',
        icon: 'point_of_sale',
        destination: { kind: 'route', value: '/app/admin/sellers' },
      },
      {
        id: 'outlets',
        labelKey: 'nav.admin.outlets',
        icon: 'storefront',
        destination: { kind: 'route', value: '/app/admin/outlets' },
      },
      {
        id: 'terminals',
        labelKey: 'nav.admin.terminals',
        icon: 'terminal',
        destination: { kind: 'route', value: '/app/admin/terminals' },
      },
      {
        id: 'sessions',
        labelKey: 'nav.admin.sessions',
        icon: 'event_available',
        destination: { kind: 'route', value: '/app/admin/sessions' },
      },
      {
        id: 'settings',
        labelKey: 'nav.admin.settings',
        icon: 'settings',
        destination: { kind: 'route', value: '/app/admin/settings' },
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
