import {
  PLATFORM_NAVIGATION,
  TENANT_ADMIN_FOOTER,
  TENANT_ADMIN_NAVIGATION,
  TENANT_ADMIN_USER_GUIDE_URL,
} from './private-navigation.model';

/**
 * Le menu admin est réparti entre plusieurs sections et un bas de menu. Ces assertions portent sur
 * ce que contiennent les entrées, pas sur la collection qui les héberge — les chercher partout
 * évite de les réécrire à chaque fois qu'une entrée change de zone.
 */
const adminItem = (id: string) =>
  [...TENANT_ADMIN_NAVIGATION.flatMap(section => section.items), ...TENANT_ADMIN_FOOTER].find(
    item => item.id === id,
  );

describe('PLATFORM_NAVIGATION', () => {
  it('groups the Super Admin platform navigation by operational responsibility', () => {
    const groups = PLATFORM_NAVIGATION[0].items;

    expect(groups.map(group => group.labelKey)).toEqual([
      'platform.nav.dashboard',
      'platform.nav.tenantsGroup',
      'platform.nav.operations',
      'platform.nav.accessSecurity',
      'platform.nav.audit',
      'platform.nav.references',
      'platform.nav.archives',
      'platform.nav.communicationSupport',
      'platform.nav.tchala',
      'platform.nav.platformReports',
    ]);
    expect(groups.every(group => group.children?.length || group.destination)).toBe(true);
  });

  it('keeps operations focused on runtime actions', () => {
    const operations = PLATFORM_NAVIGATION[0].items.find(group => group.id === 'operations');

    expect(operations?.children?.map(child => child.labelKey)).toEqual([
      'platform.nav.overview',
      'platform.nav.draws',
      'platform.nav.drawResults',
      'platform.nav.salesSimulations',
      'platform.nav.jobs',
      'platform.nav.cache',
      'platform.nav.pageEngine',
    ]);
    expect(operations?.children?.map(child => child.destination?.value)).toEqual([
      '/app/platform/ops',
      '/app/platform/ops/draws',
      '/app/platform/ops/draw-results',
      '/app/platform/ops/sales-simulations',
      '/app/platform/ops/jobs',
      '/app/platform/ops/cache',
      '/app/platform/ops/page-engine',
    ]);
  });

  it('separates permissions, roles and user access in access control', () => {
    const access = PLATFORM_NAVIGATION[0].items.find(group => group.id === 'access');

    expect(access?.children?.map(child => child.labelKey)).toEqual([
      'platform.nav.permissions',
      'platform.nav.roles',
      'platform.nav.accessUsers',
      'platform.nav.superAdmins',
      'platform.nav.users',
      'platform.nav.backendKeys',
    ]);
    expect(access?.children?.map(child => child.destination?.value)).toEqual([
      '/app/platform/access/permissions',
      '/app/platform/access/roles',
      '/app/platform/access/users',
      '/app/platform/super-admins',
      '/app/platform/tenant-admins',
      '/app/platform/access/backend-keys',
    ]);
  });

  it('separates audit and archives from runtime operations', () => {
    const audit = PLATFORM_NAVIGATION[0].items.find(group => group.id === 'audit');
    const archives = PLATFORM_NAVIGATION[0].items.find(group => group.id === 'archives');

    expect(audit?.children?.map(child => child.destination?.value)).toEqual([
      '/app/platform/audit',
      '/app/platform/audit/entity-history',
    ]);
    expect(archives?.children?.map(child => child.destination?.value)).toEqual([
      '/app/platform/archives',
      '/app/platform/archives/runs',
      '/app/platform/archives/issues',
      '/app/platform/archives/legal-holds',
      '/app/platform/archives/partitions',
      '/app/platform/archives/purges',
    ]);
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
      '/app/platform/catalog/settings',
      '/app/platform/catalog/themes',
      '/app/platform/catalog/translations',
      '/app/platform/catalog/page-model-templates',
    ]);
  });

  it('groups communication and support management together without duplicating support', () => {
    const supportAndContent = PLATFORM_NAVIGATION[0].items.find(
      group => group.id === 'support-and-content',
    );

    expect(supportAndContent?.labelKey).toBe('platform.nav.communicationSupport');
    expect(supportAndContent?.children?.map(child => child.labelKey)).toEqual([
      'platform.nav.inAppNotifications',
      'platform.nav.contactRequests',
      'platform.nav.news',
      'platform.nav.contactConfig',
      'platform.nav.communicationOutbox',
      'platform.nav.communicationTests',
    ]);
    expect(supportAndContent?.children?.[0].destination?.value).toBe(
      '/app/platform/communication/notifications',
    );
  });

  it('exposes the tenant admin notification center under my company', () => {
    const company = adminItem('company');

    expect(company?.children?.map(child => child.labelKey)).toContain(
      'nav.admin.company_notifications',
    );
    expect(company?.children?.find(child => child.id === 'company-notifications')?.destination?.value)
      .toBe('/app/admin/notifications');
  });

  it('points company identity to the business profile and does not expose address separately', () => {
    const setup = adminItem('setup');
    const company = adminItem('company');

    expect(setup?.activeRoutes).not.toContain('/app/admin/business-profile');
    expect(company?.destination?.value).toBe('/app/admin/business-profile');
    expect(company?.children?.find(child => child.id === 'company-identity')?.destination?.value)
      .toBe('/app/admin/business-profile');
    expect(company?.children?.map(child => child.id)).not.toContain('company-address');
  });

  it('keeps generic promotions hidden from the V0 tenant admin sidenav', () => {
    const items = TENANT_ADMIN_NAVIGATION.flatMap(section => section.items);

    expect(items.map(item => item.id)).toContain('maryaj-gratis');
    expect(items.map(item => item.id)).not.toContain('promotions');
  });

  it('points tenant admin help to the published user guide', () => {
    const help = adminItem('help');

    expect(help?.destination).toEqual({
      kind: 'url',
      value: TENANT_ADMIN_USER_GUIDE_URL,
    });
  });

  it('exposes tenant page models under my company', () => {
    const company = adminItem('company');

    expect(company?.children?.map(child => child.labelKey)).toContain(
      'nav.admin.company_page_models',
    );
    expect(company?.children?.find(child => child.id === 'company-page-models')?.destination?.value)
      .toBe('/app/admin/pagemodels');
  });

  it('exposes business days under my company', () => {
    const setup = adminItem('setup');
    const company = adminItem('company');

    expect(setup?.activeRoutes).not.toContain('/app/admin/business-days');
    expect(company?.children?.map(child => child.labelKey)).toContain(
      'nav.admin.company_business_days',
    );
    expect(company?.children?.find(child => child.id === 'company-business-days')?.destination?.value)
      .toBe('/app/admin/business-days');
  });

  it('keeps tenant settings under my company instead of using the signed-in admin account menu', () => {
    const setup = adminItem('setup');
    const company = adminItem('company');
    const companySettings = company?.children?.find(child => child.id === 'company-settings');

    expect(setup?.activeRoutes).not.toContain('/app/admin/settings');
    expect(companySettings?.destination?.value).toBe('/app/admin/company/settings');
    expect(companySettings?.activeRoutes).toContain('/app/admin/company/settings/config');
  });

  it('keeps games overview active for pricing details', () => {
    const games = adminItem('games');
    const overview = games?.children?.find(child => child.id === 'games-overview');

    expect(overview?.activeMatch).toBe('exact');
    expect(overview?.activeRoutes).toContain('/app/admin/pricing');
  });
});
