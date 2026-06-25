import { PLATFORM_NAVIGATION } from './private-navigation.model';

describe('PLATFORM_NAVIGATION', () => {
  it('groups the Super Admin platform navigation by operational responsibility', () => {
    const groups = PLATFORM_NAVIGATION[0].items;

    expect(groups.map(group => group.labelKey)).toEqual([
      'platform.nav.dashboard',
      'platform.nav.tenantsGroup',
      'platform.nav.references',
      'platform.nav.operations',
      'platform.nav.supportAndContent',
      'platform.nav.tchala',
      'platform.nav.accessSecurity',
      'platform.nav.platformReports',
    ]);
    expect(groups.every(group => group.children?.length || group.destination)).toBe(true);
  });

  it('keeps the dashboard group split between commercial and ops dashboards', () => {
    const dashboard = PLATFORM_NAVIGATION[0].items.find(group => group.id === 'dashboard');

    expect(dashboard?.labelKey).toBe('platform.nav.dashboard');
    expect(dashboard?.children?.map(child => child.labelKey)).toEqual([
      'platform.nav.opsDashboard',
      'platform.nav.commercialDashboard',
    ]);
    expect(dashboard?.children?.map(child => child.destination?.value)).toEqual([
      '/app/platform',
      '/app/platform/dashboard',
    ]);
  });

  it('keeps catalog entries under referentials with frontend routes', () => {
    const references = PLATFORM_NAVIGATION[0].items.find(group => group.id === 'references');

    expect(references?.labelKey).toBe('platform.nav.references');
    expect(references?.children?.map(child => child.destination?.value)).toEqual([
      '/app/platform/catalog/games',
      '/app/platform/catalog/draw-channels',
      '/app/platform/catalog/draw-channel-games',
      '/app/platform/catalog/result-slots',
      '/app/platform/catalog/result-slot-calendars',
      '/app/platform/catalog/plans',
      '/app/platform/catalog/pricing',
      '/app/platform/catalog/settings',
      '/app/platform/catalog/themes',
      '/app/platform/catalog/translations',
      '/app/platform/catalog/page-model-templates',
    ]);
  });

  it('groups support and content management together', () => {
    const supportAndContent = PLATFORM_NAVIGATION[0].items.find(
      group => group.id === 'support-and-content',
    );

    expect(supportAndContent?.children?.map(child => child.labelKey)).toEqual([
      'platform.nav.contactRequests',
      'platform.nav.news',
      'platform.nav.inAppNotifications',
      'platform.nav.contactConfig',
    ]);
    expect(supportAndContent?.children?.[0].destination?.value).toBe(
      '/app/platform/contact-requests',
    );
  });
});
