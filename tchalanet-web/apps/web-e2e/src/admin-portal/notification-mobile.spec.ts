import { expect, test } from '../support/fixtures';
import { tenantAdminPrivateBootstrap } from '../support/api-stub';
import { credsFor } from '../support/env';

test.describe('Admin notification bell on mobile', () => {
  test.use({ viewport: { width: 390, height: 844 }, isMobile: true });

  test.beforeEach(async ({ loginPage, apiStub }) => {
    const creds = credsFor('admin');
    if (!creds) {
      test.skip(true, 'TCH_E2E_ADMIN_EMAIL/PASSWORD not configured');
      return;
    }

    await apiStub.privateBootstrap(tenantAdminPrivateBootstrap);
    await loginPage.login(creds);
  });

  test('keeps the bell visible and opens a menu contained in the viewport', async ({
    page,
    privateShell,
  }) => {
    await expect(privateShell.notificationBell).toBeVisible();
    await privateShell.notificationBell.click();
    await expect(privateShell.notificationMenu).toBeVisible();

    const menu = await privateShell.notificationMenu.boundingBox();
    expect(menu).not.toBeNull();
    expect(menu!.x).toBeGreaterThanOrEqual(0);
    expect(menu!.x + menu!.width).toBeLessThanOrEqual(page.viewportSize()!.width);
  });
});
