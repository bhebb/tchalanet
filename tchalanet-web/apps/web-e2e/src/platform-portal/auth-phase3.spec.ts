import { expect, test } from '../support/fixtures';
import { tenantAdminPrivateBootstrap } from '../support/api-stub';
import { credsFor } from '../support/env';

test.describe('Phase 3 — platform auth completion', () => {
  test('super admin can logout from an authenticated shell', async ({
    page,
    loginPage,
    privateShell,
    apiStub,
  }) => {
    const creds = credsFor('superAdmin');
    test.skip(!creds, 'TCH_E2E_SUPERADMIN_EMAIL/PASSWORD not configured');

    void apiStub;
    await loginPage.login(creds!);
    await expect(page).toHaveURL(/\/app\/platform\b/);

    await privateShell.logoutFromShell();
    await expect(loginPage.root).toBeVisible();
  });

  test('support tenant round-trip opens admin support mode then returns platform', async ({
    page,
    loginPage,
    privateShell,
    supportTenantPage,
    apiStub,
  }) => {
    const creds = credsFor('superAdmin');
    test.skip(!creds, 'TCH_E2E_SUPERADMIN_EMAIL/PASSWORD not configured');

    await apiStub.portalHandoff('ADMIN', 'http://localhost:4302', '/app/admin');
    await apiStub.tenants([
      { tenantId: 'stub-tenant-1', code: 'ACME', name: 'Acme Lottery', status: 'ACTIVE' },
    ]);

    await loginPage.login(creds!);
    await supportTenantPage.goto();
    await supportTenantPage.openAccessForFirstTenant();

    await apiStub.privateBootstrap(tenantAdminPrivateBootstrap);
    await supportTenantPage.confirmAccess();

    await expect(page).toHaveURL(/localhost:4302\/app\/admin\b/);
    await expect(privateShell.supportBanner).toBeVisible();
    await expect(privateShell.supportTenantName).toContainText('Acme Lottery');

    await privateShell.supportReturn.click();
    await expect(page).toHaveURL(/localhost:4303\/app\/platform\b/);
    await expect(privateShell.supportBanner).toHaveCount(0);
  });
});
