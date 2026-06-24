import { appRoutes } from './app.routes';
import { adminRoutes } from './features/private/admin/admin.routes';
import { sellerTerminalRoutes } from './features/private/seller-terminal/seller-terminal.routes';
import { platformRoutes } from './features/private/platform/platform.routes';

describe('appRoutes', () => {
  describe('private spaces use lazy loading', () => {
    it.each(['app/platform', 'app/admin', 'app/cashier'])('%s has loadComponent and loadChildren', path => {
      const route = appRoutes.find(r => r.path === path);
      expect(route?.loadComponent).toBeDefined();
      expect(route?.component).toBeUndefined();
      expect(route?.loadChildren).toBeDefined();
      expect(route?.children).toBeUndefined();
    });

    it.each(['app/platform', 'app/admin', 'app/cashier'])('%s has exactly one canActivate guard', path => {
      const route = appRoutes.find(r => r.path === path);
      expect(route?.canActivate).toHaveLength(1);
    });
  });

  it('public space uses lazy loadComponent and loadChildren', () => {
    const route = appRoutes.find(r => r.path === 'public');
    expect(route?.loadComponent).toBeDefined();
    expect(route?.loadChildren).toBeDefined();
  });

  it('redirects the root path to public', () => {
    const root = appRoutes.find(r => r.path === '' && r.redirectTo === 'public');
    expect(root).toBeDefined();
  });

  describe('platformRoutes', () => {
    it('declares all 7 paths', () => {
      const paths = platformRoutes.map(r => r.path);
      expect(paths).toEqual(
        expect.arrayContaining(['', 'tenants', 'tenant-provisioning', 'contact-requests', 'news', 'notifications', 'ops']),
      );
    });

    it('all routes use loadComponent', () => {
      platformRoutes.forEach(r => {
        expect(r.loadComponent).toBeDefined();
      });
    });
  });

  describe('adminRoutes', () => {
    it('declares the admin paths', () => {
      const paths = adminRoutes.map(r => r.path);
      expect(paths).toEqual(
        expect.arrayContaining(['', 'onboarding', 'users', 'seller-terminals', 'settings']),
      );
    });

    it('all routes use loadComponent', () => {
      adminRoutes.forEach(r => {
        expect(r.loadComponent).toBeDefined();
      });
    });
  });

  describe('cashierRoutes', () => {
    it('declares dashboard and sell routes', () => {
      const paths = sellerTerminalRoutes.map(r => r.path);
      expect(paths).toEqual(expect.arrayContaining(['', 'sell']));
    });
  });
});
