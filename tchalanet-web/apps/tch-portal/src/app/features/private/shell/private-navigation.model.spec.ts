import { PLATFORM_NAVIGATION } from './private-navigation.model';

describe('PLATFORM_NAVIGATION', () => {
  it('groups the Super Admin platform navigation by operational responsibility', () => {
    const groups = PLATFORM_NAVIGATION[0].items;

    expect(groups.map(group => group.labelKey)).toEqual([
      'platform.nav.overview',
      'platform.nav.tenantsGroup',
      'platform.nav.references',
      'platform.nav.operations',
      'platform.nav.accessRights',
      'platform.nav.communication',
      'platform.nav.reports',
    ]);
    expect(groups.every(group => group.children?.length)).toBe(true);
  });

  it('keeps catalog entries under referentials with frontend routes', () => {
    const references = PLATFORM_NAVIGATION[0].items.find(group => group.id === 'references');

    expect(references?.labelKey).toBe('platform.nav.references');
    expect(references?.children?.map(child => child.destination?.value)).toEqual([
      '/app/platform/catalog/games',
      '/app/platform/catalog/draw-channels',
      '/app/platform/catalog/result-slots',
      '/app/platform/catalog/plans-pricing',
      '/app/platform/catalog/settings',
      '/app/platform/catalog/themes',
      '/app/platform/catalog/translations',
      '/app/platform/catalog/page-model-templates',
    ]);
  });

  it('groups contact management under communication', () => {
    const communication = PLATFORM_NAVIGATION[0].items.find(group => group.id === 'communication');

    expect(communication?.children?.map(child => child.labelKey)).toEqual([
      'platform.nav.inAppNotifications',
      'platform.nav.contactManagement',
      'platform.nav.news',
    ]);
    expect(communication?.children?.[1].destination?.value).toBe('/app/platform/communication/contacts');
  });
});
