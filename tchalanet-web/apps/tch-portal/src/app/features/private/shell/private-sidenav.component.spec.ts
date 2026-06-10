import { CASHIER_NAVIGATION, PLATFORM_NAVIGATION, TENANT_ADMIN_NAVIGATION } from './private-navigation.model';

describe('Private navigation constants', () => {
  it('PLATFORM_NAVIGATION contains all expected item ids', () => {
    const ids = PLATFORM_NAVIGATION.flatMap(s => s.items.map(i => i.id));
    expect(ids).toContain('platform-dashboard');
    expect(ids).toContain('tenants');
    expect(ids).toContain('tenant-provisioning');
    expect(ids).toContain('contact-requests');
    expect(ids).toContain('news');
    expect(ids).toContain('notifications');
    expect(ids).toContain('ops');
  });

  it('TENANT_ADMIN_NAVIGATION contains all expected item ids', () => {
    const ids = TENANT_ADMIN_NAVIGATION.flatMap(s => s.items.map(i => i.id));
    expect(ids).toContain('admin-dashboard');
    expect(ids).toContain('onboarding');
    expect(ids).toContain('users');
    expect(ids).toContain('sellers');
    expect(ids).toContain('outlets');
    expect(ids).toContain('terminals');
    expect(ids).toContain('sessions');
    expect(ids).toContain('settings');
  });

  it('CASHIER_NAVIGATION contains cashier-dashboard and cashier-sell', () => {
    const ids = CASHIER_NAVIGATION.flatMap(s => s.items.map(i => i.id));
    expect(ids).toContain('cashier-dashboard');
    expect(ids).toContain('cashier-sell');
  });

  it('all navigation items have id, labelKey and destination', () => {
    [...PLATFORM_NAVIGATION, ...TENANT_ADMIN_NAVIGATION, ...CASHIER_NAVIGATION].forEach(section => {
      expect(section.id).toBeTruthy();
      expect(section.titleKey).toBeTruthy();
      section.items.forEach(item => {
        expect(item.id).toBeTruthy();
        expect(item.labelKey).toBeTruthy();
        expect(item.destination).toBeDefined();
      });
    });
  });

  it('platform-dashboard uses exact activeMatch', () => {
    const item = PLATFORM_NAVIGATION.flatMap(s => s.items).find(i => i.id === 'platform-dashboard');
    expect(item?.activeMatch).toBe('exact');
  });

  it('admin-dashboard uses exact activeMatch', () => {
    const item = TENANT_ADMIN_NAVIGATION.flatMap(s => s.items).find(i => i.id === 'admin-dashboard');
    expect(item?.activeMatch).toBe('exact');
  });

  it('cashier-dashboard uses exact activeMatch', () => {
    const item = CASHIER_NAVIGATION.flatMap(s => s.items).find(i => i.id === 'cashier-dashboard');
    expect(item?.activeMatch).toBe('exact');
  });
});
